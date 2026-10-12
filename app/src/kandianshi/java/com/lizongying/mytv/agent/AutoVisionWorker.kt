package com.lizongying.mytv.agent

import android.content.Context
import android.net.ConnectivityManager
import android.os.SystemClock
import androidx.work.*
import com.lizongying.mytv.TVList
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.TimeUnit

class AutoVisionWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result=withContext(Dispatchers.IO) {
        val c=applicationContext; val settings=AutoVisionSettings(c); val config=AgentConfigStore(c)
        val cfg=config.load()
        if(!settings.enabled() || !cfg.enabled || !cfg.aiEnabled || config.apiKey().isBlank()) {
            settings.status("等待配置并启用 AI，或自动巡检已关闭"); return@withContext Result.success()
        }
        if(settings.apiPaused()) { settings.status("图片接口暂不可用，冷却后再试"); return@withContext Result.success() }
        val quota=VisionStore(c)
        if(quota.used()>=quota.limit()) { settings.status("今日图片额度已用完，等待下一日补查"); return@withContext Result.success() }
        if(DecoderGate.foregroundWanted() || !VisionStore.runLock.tryLock()) {
            settings.status("正在观看或手动巡检，空闲后自动继续"); return@withContext Result.retry()
        }
        val lease=Any(); val run=UUID.randomUUID().toString()
                try {
            fun networkOk(): Boolean {
                val cm=c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                @Suppress("DEPRECATION")
                return cm.activeNetworkInfo?.isConnected == true && (!settings.wifiOnly() || !cm.isActiveNetworkMetered)
            }
            fun allowed(): Boolean = settings.enabled() && config.load().let { it.enabled && it.aiEnabled } &&
                networkOk() && DecoderGate.backgroundAllowed(lease)
            val memory=AgentRepository.prepareMemory(c)
            val routes=VisionPolicy.interleave(TVList.load(c,64).map { ch -> ch.urls.map { VisionRoute(ch.title,it) } })
            val active=routes.map { it.url }.toSet()
            val progress=VisionStore(c,"autoPlan")
            var plan=progress.begin(routes,false)
            val reviews=java.util.ArrayDeque(memory.dueAutoReviews().filter { it.url in active })
            val visited=mutableSetOf<String>()
            val deadline=SystemClock.elapsedRealtime()+240_000
            var calls=0; var checked=0; var blocked=0
            while(checked<40 && calls<3 && SystemClock.elapsedRealtime()<deadline-55_000) {
                ensureActive()
                if(!settings.enabled() || !config.load().let { it.enabled && it.aiEnabled }) break
                if(DecoderGate.foregroundWanted()) throw ScanDeferred("正在观看，空闲后自动继续")
                val review=if(reviews.isNotEmpty()) reviews.removeFirst() else null
                val route=review ?: plan.next() ?: break
                val fromPlan=review==null
                if(!visited.add(route.url)) {
                    if(fromPlan) plan=progress.advanceCached(plan,route)
                    continue
                }
                checked++
                val excluded=route.url !in active || MemoryPolicy.id(route.url) in memory.adIds()
                val cached=if(excluded) null else memory.cached(route.url)
                val needs=!excluded && AutoVisionPolicy.needsInspection(cached,memory.autoReview(route.url),System.currentTimeMillis())
                if(!needs) {
                    if(fromPlan) plan=progress.advanceCached(plan,route)
                    continue
                }
                if(quota.used()>=quota.limit()) break
                if(!DecoderGate.tryBackground(lease)) throw ScanDeferred("用户开始播放，空闲后自动继续")
                val frames=try {
                    settings.status("后台取样："+route.title+"；今日 "+quota.used()+"/"+quota.limit())
                    OffscreenVideoSampler(c).sample(route,::allowed)
                } catch(e: ScanDeferred) { throw e }
                catch(e: CancellationException) { throw e }
                catch(_: Exception) { emptyList() }
                finally { DecoderGate.releaseLease(lease) }
                ensureActive()
                if(frames.size!=VisionPolicy.FRAME_COUNT) {
                    memory.putVision(route,VisionVerdict("unavailable","后台未能获取三张可靠画面，保留线路；6 小时后可重查"))
                    if(fromPlan) plan=progress.advanceCached(plan,route)
                    continue
                }
                if(!settings.enabled() || !config.load().let { it.enabled && it.aiEnabled }) break
                if(!networkOk()) throw ScanDeferred("等待允许的网络后自动继续")
                val verdict=try {
                    calls++
                    settings.status("后台识别："+route.title+"；三张节目图将发往已配置 API")
                    VisionClient(c).inspect(route.title,frames)
                } catch(e: CancellationException) { throw e }
                catch(_: Exception) {
                    settings.pauseApi()
                    settings.status("图片 API 暂不可用，停止本批并冷却 6 小时；频道与进度保留")
                    AgentRepository.log(c,"自动画面巡检接口失败，本批停止，6 小时后复查")
                    return@withContext Result.success()
                }
                ensureActive()
                if(!settings.enabled() || !config.load().let { it.enabled && it.aiEnabled }) break
                if(memory.recordAuto(route,verdict,run,settings.autoBlock())) {
                    blocked++; AgentRepository.memoryChanged(c)
                    AgentRepository.log(c,"自动两轮确认纯广告，已永久屏蔽："+route.title.take(80))
                }
                if(fromPlan) plan=progress.advanceCached(plan,route)
            }
            val more=(!plan.done || memory.pendingAutoReviews().any { it.url in active }) &&
                quota.used()<quota.limit() && settings.enabled()
            settings.status("本批检查 "+checked+" 条，新识别 "+calls+" 条，自动屏蔽 "+blocked+" 条；"+
                if(quota.used()>=quota.limit()) "今日额度已用完" else if(more) "约 30 分钟后空闲续查" else "等待下一轮")
            if(more) AutoVisionScheduler.continueSoon(c)
            Result.success()
        } catch(e: ScanDeferred) {
            settings.status(e.message.orEmpty()); Result.retry()
        } catch(e: CancellationException) {
            settings.status("自动巡检已暂停，已完成记忆和进度保留"); throw e
        } catch(_: Exception) {
            settings.status("本轮未完成，保留线路和进度，稍后重试")
            if(runAttemptCount<2) Result.retry() else Result.success()
        } finally {
            DecoderGate.releaseLease(lease); VisionStore.runLock.unlock()
        }
    }
}

object AutoVisionScheduler {
    private const val PERIODIC="auto-vision-periodic"
    private const val ONCE="auto-vision-once"
    private fun constraints(c: Context)=Constraints.Builder().setRequiredNetworkType(
        if(AutoVisionSettings(c).wifiOnly()) NetworkType.UNMETERED else NetworkType.CONNECTED).build()
    private fun request(c: Context,minutes: Long)=OneTimeWorkRequestBuilder<AutoVisionWorker>().setConstraints(constraints(c))
        .setInitialDelay(minutes,TimeUnit.MINUTES).setBackoffCriteria(BackoffPolicy.LINEAR,15,TimeUnit.MINUTES).build()
    fun schedule(c: Context) {
        val cfg=AgentConfigStore(c).load(); val wm=WorkManager.getInstance(c)
        if(!cfg.enabled || !cfg.aiEnabled || !AutoVisionSettings(c).enabled()) {
            wm.cancelUniqueWork(PERIODIC); wm.cancelUniqueWork(ONCE); return
        }
        val task=PeriodicWorkRequestBuilder<AutoVisionWorker>(cfg.intervalHours.toLong(),TimeUnit.HOURS)
            .setInitialDelay(5,TimeUnit.MINUTES).setConstraints(constraints(c))
            .setBackoffCriteria(BackoffPolicy.LINEAR,15,TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(PERIODIC,ExistingPeriodicWorkPolicy.UPDATE,task)
    }
    fun nudge(c: Context) {
        val cfg=AgentConfigStore(c).load()
        if(AutoVisionSettings(c).enabled() && cfg.enabled && cfg.aiEnabled)
            WorkManager.getInstance(c).enqueueUniqueWork(ONCE,ExistingWorkPolicy.KEEP,request(c,2))
    }
    fun continueSoon(c: Context) {
        WorkManager.getInstance(c).enqueueUniqueWork(ONCE,ExistingWorkPolicy.APPEND_OR_REPLACE,request(c,30))
    }
}
