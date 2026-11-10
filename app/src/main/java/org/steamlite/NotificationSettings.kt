package org.steamlite

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun NotificationSettings(notifications: ChatNotifications) {
    val context = LocalContext.current
    var feedback by remember { mutableStateOf("") }
    var granted by remember { mutableStateOf(notifications.allowed()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        feedback = if (ok) "已获通知权限" else "未获通知权限，可在系统设置中开启"
    }

    SettingsRow(
        Glyph.Bell,
        "系统通知",
        value = if (granted) "已开启" else "未开启",
        onClick = {
            if (Build.VERSION.SDK_INT >= 33 && !notifications.allowed()) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        }
    )
    SettingsRow(
        Glyph.Send,
        "发送测试通知",
        value = null,
        showDivider = false,
        onClick = {
            feedback = if (notifications.test()) "本机测试通知已提交" else "通知被关闭，请检查系统权限和通知渠道"
            granted = notifications.allowed()
        }
    )
    if (feedback.isNotEmpty()) {
        MetaLine(feedback, Glyph.Info)
    }
}
