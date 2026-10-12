package com.lizongying.mytv.agent

import android.content.Context
import androidx.work.*
import com.lizongying.mytv.TVList
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.coroutineContext

/** Same worker class name lets existing queued work migrate without calling the old design API. */
class LogoWorker(c: Context, params: WorkerParameters): CoroutineWorker(c, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val store=AgentConfigStore(applicationContext)
        fun allowed()=store.load().let { it.enabled && it.logoLookupEnabled } && !DecoderGate.foregroundWanted()
        if(!allowed() || !runLock.tryLock()) return@withContext Result.success()
        try {
            RealLogoStore.enqueue(applicationContext,TVList.load(applicationContext).map { it.title to it.logo })
            var requests=0;var saved=0
            for((title,urls) in RealLogoStore.pending(applicationContext)) {
                coroutineContext.ensureActive()
                if(!allowed() || requests>=24) break
                RealLogoStore.attempt(applicationContext,title)
                for(url in urls) {
                    coroutineContext.ensureActive()
                    if(!allowed() || requests>=24) break
                    requests++
                    val good=runCatching { RealLogoStore.put(applicationContext,title,url,HttpFetch.get(url,256*1024).bytes) }.getOrDefault(false)
                    if(good) { saved++;break } else RealLogoStore.failure(applicationContext,url)
                }
            }
            if(saved>0) AgentRepository.log(applicationContext,"已补齐 $saved 个真实频道台标，原始图案已缓存")
            if(allowed() && RealLogoStore.pending(applicationContext).isNotEmpty()) LogoScheduler.nudge(applicationContext,true)
            Result.success()
        } catch(e: CancellationException) { throw e }
        catch(_: Exception) { Result.success() }
        finally { runLock.unlock() }
    }
    companion object { private val runLock=Mutex() }
}

object LogoScheduler {
    private fun constraints()=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun schedule(c: Context) {
        RealLogoStore.applyLimit(c)
        val config=AgentConfigStore(c).load();val wm=WorkManager.getInstance(c)
        if(!config.enabled || !config.logoLookupEnabled) {
            wm.cancelUniqueWork("channel-logo-periodic");wm.cancelUniqueWork("channel-logo-idle");return
        }
        wm.enqueueUniquePeriodicWork("channel-logo-periodic",ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<LogoWorker>(6,TimeUnit.HOURS).setConstraints(constraints()).build())
        nudge(c)
    }
    fun nudge(c: Context,continuation: Boolean=false) {
        val config=AgentConfigStore(c).load()
        if(!config.enabled || !config.logoLookupEnabled || DecoderGate.foregroundWanted()) return
        val request=OneTimeWorkRequestBuilder<LogoWorker>().setConstraints(constraints()).apply {
            if(continuation) setInitialDelay(15,TimeUnit.MINUTES)
        }.build()
        WorkManager.getInstance(c).enqueueUniqueWork("channel-logo-idle",
            if(continuation) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,request)
    }
}
