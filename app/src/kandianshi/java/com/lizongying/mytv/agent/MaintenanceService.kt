package com.lizongying.mytv.agent

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lizongying.mytv.MainActivity
import com.lizongying.mytv.R
import kotlinx.coroutines.*

class MaintenanceService: Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var job: Job?=null
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        if(intent?.action=="stop") { setEnabled(this,false); stopSelf(); return START_NOT_STICKY }
        if(!enabled(this)) { stopSelf(); return START_NOT_STICKY }
        val nm=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(NotificationChannel("maintenance","后台频道维护",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,1,Intent(this,MaintenanceService::class.java).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        startForeground(410,NotificationCompat.Builder(this,"maintenance").setSmallIcon(R.drawable.kan_logo)
            .setContentTitle("看电视 · 后台维护已开启").setContentText("空闲时找新频道、复核无效线路和广告；观看优先")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(0,"停止常驻",stop).build())
        running=true
        if(job?.isActive!=true) job=scope.launch {
            var nextHealth=0L
            while(isActive && enabled(this@MaintenanceService)) {
                if(!DecoderGate.foregroundWanted() && HealthScanner.networkConnected(this@MaintenanceService) && AgentConfigStore(this@MaintenanceService).load().enabled) {
                    val wake=(getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"kandianshi:maintenance")
                    try {
                        wake.acquire(780_000)
                        AgentRepository.maintain(this@MaintenanceService,false,true,true)
                        if(SystemClock.elapsedRealtime()>=nextHealth && !DecoderGate.foregroundWanted()) {
                            val generation=getSharedPreferences("agent_state",MODE_PRIVATE).getLong("scanGeneration",0)
                            val outcome=HealthScanner.scan(this@MaintenanceService,true,generation)
                            nextHealth=SystemClock.elapsedRealtime()+if(outcome==HealthScanner.Outcome.COMPLETE) 21_600_000 else 900_000
                        }
                        if(!DecoderGate.foregroundWanted()) AutoVisionRunner.run(this@MaintenanceService)
                    } catch(e: CancellationException) { currentCoroutineContext().ensureActive() }
                    catch(_: Exception) { /* Existing queue, source and API backoffs remain in force. */ }
                    finally { if(wake.isHeld) wake.release() }
                }
                delay(900_000)
            }
        }
        return START_STICKY
    }
    override fun onTimeout(startId: Int,fgsType: Int) { stopSelf() }
    override fun onDestroy() {
        running=false; scope.cancel()
        Thread({ BufferedState.flush() },"maintenance-final-checkpoint").start()
        super.onDestroy()
    }
    companion object {
        @Volatile var running=false; private set
        fun enabled(c: Context)=c.getSharedPreferences("background_maintenance",MODE_PRIVATE).getBoolean("enabled",true)
        fun setEnabled(c: Context,on: Boolean) { c.getSharedPreferences("background_maintenance",MODE_PRIVATE).edit().putBoolean("enabled",on).apply() }
        /** Called only while an Activity is visible; boot uses WorkManager fallback. */
        fun ensure(c: Context) { if(enabled(c)) runCatching { ContextCompat.startForegroundService(c,Intent(c,MaintenanceService::class.java)) } }
        fun toggle(c: Context) {
            val on=!enabled(c); setEnabled(c,on)
            if(on) ensure(c) else c.stopService(Intent(c,MaintenanceService::class.java))
        }
    }
}
