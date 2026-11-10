package org.steamlite

import android.app.*
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class LiteApplication : Application() {
    val model by lazy { LiteModel(this) }
}

/** Owns the ongoing messaging notification while the shared connection is active. */
class ConnectionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("connection", "后台消息连接", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun notification(text: String) = Notification.Builder(this, "connection")
            .setSmallIcon(R.drawable.ic_launcher).setContentTitle("Steam Lite")
            .setContentText(text).setContentIntent(open).setOngoing(true).build()
        startForeground(42, notification("保持 Steam 消息连接"))
        val steam = (application as LiteApplication).model.steam
        scope.launch { while (isActive) { delay(30_000); steam.resumeSession() } }
        scope.launch { steam.status.collectLatest { status ->
            if (!steam.online.value && steam.owner.isBlank() && !steam.busy.value) stopSelf()
            else manager.notify(42, notification(if (steam.online.value) "Steam 已连接 · 后台接收消息" else status))
        } }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
