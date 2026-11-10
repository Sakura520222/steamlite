package org.steamlite

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class ChatNotifications(private val context: Context) {
    @Volatile var appVisible = true
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val channelId = "steam_messages"
    init {
        manager.createNotificationChannel(NotificationChannel(channelId, "Steam 私聊消息", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "连接仍在线时收到的私聊；不保证后台永久在线"
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        })
    }
    fun allowed(): Boolean = manager.areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        manager.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE

    fun incoming(peer: Long) { if(!appVisible) post("peer:$peer", "收到新的 Steam 私聊消息") }
    fun test(): Boolean = post("local_test", "本机通知测试：未向 Steam 好友发送消息")
    private fun post(tag: String, text: String): Boolean {
        if(!allowed()) return false
        val open = Intent(context, MainActivity::class.java).putExtra("open_chat", true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle("Steam Lite")
            .setContentText(text).setContentIntent(pending).setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_MESSAGE)
            .setOnlyAlertOnce(true).build()
        return runCatching { manager.notify(tag, 1, notification); true }.getOrDefault(false)
    }
    fun clear() { manager.cancelAll() }
}
