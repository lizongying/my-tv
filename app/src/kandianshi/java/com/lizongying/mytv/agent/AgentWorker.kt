package com.lizongying.mytv.agent

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class AgentWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        if (DecoderGate.foregroundWanted()) throw ScanDeferred("正在看电视，退出播放后继续找源")
        AgentRepository.maintain(applicationContext, inputData.getBoolean("force", false), true)
        AutoVisionScheduler.nudge(applicationContext)
        if(AgentRepository.discoveryPending(applicationContext)) AgentScheduler.continueIdle(applicationContext)
        Result.success()
    } catch(e: ScanDeferred) {
        applicationContext.getSharedPreferences("agent_state",Context.MODE_PRIVATE).edit()
            .putString("discoveryStatus",e.message).apply()
        Result.retry()
    } catch (e: CancellationException) { throw e }
    catch (_: Exception) {
        AgentRepository.log(applicationContext, "本轮维护未完成，现有频道保留；稍后重试")
        if (runAttemptCount < 2) Result.retry() else Result.failure()
    }
}

class SourceScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val generation = inputData.getLong("generation", 0)
    private fun isCurrent() = applicationContext.getSharedPreferences("agent_state", Context.MODE_PRIVATE).getLong("scanGeneration", 0) == generation
    override suspend fun doWork(): Result = try {
        if (!isCurrent()) Result.success() else when (HealthScanner.scan(applicationContext, inputData.getBoolean("begin", false), generation)) {
            HealthScanner.Outcome.COMPLETE -> Result.success()
            HealthScanner.Outcome.MORE -> { AgentScheduler.continueScan(applicationContext, generation); Result.success() }
            HealthScanner.Outcome.BUSY -> Result.retry()
            HealthScanner.Outcome.RETRY -> if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    } catch (e: CancellationException) {
        if (isCurrent()) applicationContext.getSharedPreferences("agent_state", Context.MODE_PRIVATE).edit().putString("scanStatus", "检测已中断，已完成结果保留；重新点击可继续").apply()
        throw e
    } catch (_: Exception) {
        AgentRepository.log(applicationContext, "全列表检测未完成，频道与检测进度保留；可重新点击继续")
        if (runAttemptCount < 2) Result.retry() else Result.failure()
    }
}

object AgentScheduler {
    private val scanLock = Any()
    private val constraints get() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun schedule(c: Context) {
        AutoVisionScheduler.schedule(c)
        LogoScheduler.schedule(c)
        val config = AgentConfigStore(c).load(); val wm = WorkManager.getInstance(c)
        if (!config.enabled) {
            listOf("source-agent-periodic","source-agent-once","source-agent-idle").forEach(wm::cancelUniqueWork)
            return
        }
        val work = PeriodicWorkRequestBuilder<AgentWorker>(config.intervalHours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints).setBackoffCriteria(BackoffPolicy.LINEAR,30,TimeUnit.SECONDS).build()
        wm.enqueueUniquePeriodicWork("source-agent-periodic", ExistingPeriodicWorkPolicy.UPDATE, work)
        runNow(c, false)
    }
    fun runNow(c: Context, force: Boolean = true) {
        val work = OneTimeWorkRequestBuilder<AgentWorker>().setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR,30,TimeUnit.SECONDS)
            .setInputData(workDataOf("force" to force)).build()
        WorkManager.getInstance(c).enqueueUniqueWork("source-agent-once", ExistingWorkPolicy.KEEP, work)
    }
    fun playbackStopped(c: Context) {
        LogoScheduler.nudge(c)
        if(!AgentConfigStore(c).load().enabled) return
        val work=OneTimeWorkRequestBuilder<AgentWorker>().setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR,30,TimeUnit.SECONDS).build()
        WorkManager.getInstance(c).enqueueUniqueWork("source-agent-idle",ExistingWorkPolicy.KEEP,work)
    }
    fun playbackStarted(c: Context) {
        WorkManager.getInstance(c).cancelUniqueWork("source-agent-idle")
        WorkManager.getInstance(c).cancelUniqueWork("channel-logo-idle")
    }
    fun continueIdle(c: Context) {
        if(!AgentConfigStore(c).load().enabled) return
        val work=OneTimeWorkRequestBuilder<AgentWorker>().setConstraints(constraints).setInitialDelay(15,TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.LINEAR,30,TimeUnit.SECONDS).build()
        WorkManager.getInstance(c).enqueueUniqueWork("source-agent-idle",ExistingWorkPolicy.APPEND_OR_REPLACE,work)
    }
    private fun scanWork(begin: Boolean, generation: Long) = OneTimeWorkRequestBuilder<SourceScanWorker>().setConstraints(constraints)
        .setInputData(workDataOf("begin" to begin, "generation" to generation)).setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
        .apply { if (!begin) setInitialDelay(45, TimeUnit.SECONDS) }.build()

    fun scanAll(c: Context) {
        synchronized(scanLock) {
            val p = c.getSharedPreferences("agent_state", Context.MODE_PRIVATE)
            val generation = p.getLong("scanGeneration", 0) + 1
            p.edit().putLong("scanGeneration", generation).apply()
            WorkManager.getInstance(c).enqueueUniqueWork("source-health-scan", ExistingWorkPolicy.REPLACE, scanWork(true, generation))
        }
    }
    internal fun continueScan(c: Context, generation: Long) {
        synchronized(scanLock) {
            if (c.getSharedPreferences("agent_state", Context.MODE_PRIVATE).getLong("scanGeneration", 0) != generation) return
            WorkManager.getInstance(c).enqueueUniqueWork("source-health-scan", ExistingWorkPolicy.APPEND_OR_REPLACE, scanWork(false, generation))
        }
    }
    fun stopScan(c: Context) {
        synchronized(scanLock) {
            val p = c.getSharedPreferences("agent_state", Context.MODE_PRIVATE)
            p.edit().putLong("scanGeneration", p.getLong("scanGeneration", 0) + 1).putString("scanStatus", "检测已停止，已完成结果保留；重新点击可继续").apply()
            WorkManager.getInstance(c).cancelUniqueWork("source-health-scan")
        }
    }
}
