package com.lizongying.mytv.agent
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.lizongying.mytv.PlaylistParser
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import java.util.concurrent.locks.ReentrantLock
object IptvOrgStore {
    data class Country(val code: String,val name: String)
    @Volatile private var catalogue: List<Country>?=null
    fun countries(c: Context): List<Country> = catalogue ?: synchronized(this) {
        catalogue ?: JSONArray(c.assets.open("iptv-org-countries.json").bufferedReader().use { it.readText() }).let { array ->
            (0 until array.length()).map { val x=array.getJSONObject(it);val code=x.getString("code");val local=Locale("",if(code=="UK") "GB" else code).getDisplayCountry(Locale.SIMPLIFIED_CHINESE);Country(code,if(local==code) x.getString("name") else local) }.sortedBy { it.name }
        }.also { catalogue=it }
    }
    fun validate(c: Context,values: List<String>)=IptvOrgPolicy.clean(values,countries(c).map { it.code }.toSet())
    fun selected(c: Context): List<String> = runCatching {
        val body=JSONObject(c.getSharedPreferences("agent_config",Context.MODE_PRIVATE).getString("config","{}").orEmpty())
        if(body.has("iptvCountries")) validate(c,AgentConfigStore.strings(body.getJSONArray("iptvCountries"))) else listOf("CN")
    }.getOrDefault(listOf("CN"))
    fun changed(c: Context,old: List<String>,next: List<String>) {
        if(old==next) return
        val state=c.getSharedPreferences("iptv_org",Context.MODE_PRIVATE).edit()
        (next-old.toSet()).forEach { state.remove("updated-$it").remove("attempt-$it") };state.putString("status","IPTV 地区已保存，等待逐条验证").apply()
        AgentRepository.memoryChanged(c);AgentScheduler.runNow(c)
    }
    fun set(c: Context,values: List<String>) {
        val next=validate(c,values);val old=selected(c);if(next==old) return
        val prefs=c.getSharedPreferences("agent_config",Context.MODE_PRIVATE);val body=JSONObject(prefs.getString("config","{}").orEmpty()).put("iptvCountries",JSONArray(next))
        check(prefs.edit().putString("config",body.toString()).commit());changed(c,old,next)
    }
    fun options(c: Context)=JSONArray(countries(c).map { JSONObject().put("code",it.code).put("name",it.name) })
    fun summary(c: Context): String {
        val selected=selected(c);if(selected.isEmpty()) return "IPTV 官方订阅：已暂停同步"
        return "IPTV 官方订阅："+selected.joinToString("、")+"\n"+c.getSharedPreferences("iptv_org",Context.MODE_PRIVATE).getString("status","每 6 小时更新官方节目单，验证后合并")
    }
    data class Result(val updated: Int,val attempted: Boolean)
    private val lock=ReentrantLock()
    suspend fun refresh(c: Context,force: Boolean,light: Boolean): Result {
        if(PlaybackRoute.buffering || !lock.tryLock()) return Result(0,false)
        try {
            val state=c.getSharedPreferences("iptv_org",Context.MODE_PRIVATE);val now=System.currentTimeMillis()
            val due=selected(c).filter { force || now-state.getLong("updated-$it",0)>=21_600_000L && now-state.getLong("attempt-$it",0)>=900_000L }
            var updated=0;var attempted=false
            for(code in if(light) due.take(1) else due) {
                currentCoroutineContext().ensureActive()
                if(PlaybackRoute.buffering || !force && !AgentConfigStore(c).load().enabled) break
                attempted=true;state.edit().putLong("attempt-$code",now).apply()
                try {
                    val reply=HttpFetch.get(IptvOrgPolicy.url(code),if(light) 512*1024 else 2*1024*1024)
                    currentCoroutineContext().ensureActive()
                    if(PlaybackRoute.buffering || !force && !AgentConfigStore(c).load().enabled) break
                    val text=reply.bytes.toString(Charsets.UTF_8);require(text.removePrefix("\uFEFF").trimStart().startsWith("#EXTM3U"))
                    val channels=PlaylistParser.parse(text,com.lizongying.mytv.TVList.channelNames(c),8,if(code=="CN") null else code)
                    CandidateAdmission.offer(c,channels)
                    state.edit().putLong("updated-$code",now).putString("status","已同步 $code："+channels.size+" 个候选频道；逐条验证后加入，约 6 小时后再查").apply();updated++
                } catch(e: CancellationException) {throw e}
                catch(_: Exception) {state.edit().putString("status","$code 本次同步未完成，15 分钟后可重试；已有频道保留").apply()}
            }
            return Result(updated,attempted)
        } finally {lock.unlock()}
    }
}
