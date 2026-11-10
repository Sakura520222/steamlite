package org.steamlite

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.webkit.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@Composable fun VoiceCallLayer(model: LiteModel) {
    val controller = model.steam.voice
    val call by controller.state.collectAsState()
    val people by model.steam.people.collectAsState()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { controller.permission(it) }
    LaunchedEffect(call.phase, call.epoch) { if(call.phase == "permission") {
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) controller.permission(true)
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    } }
    val engineActive = call.phase in setOf("preparing", "ringing", "connecting", "connected")
    if(engineActive) key(call.epoch) { VoiceAudioEngine(controller, call.epoch) }
    if(call.phase !in setOf("idle", "permission")) AlertDialog(
        onDismissRequest = { if(call.phase in setOf("ended", "error")) controller.dismiss() },
        containerColor = Surface1,
        title = { Text(people.find { it.id == call.peer }?.name ?: "Steam 语音", color = Text1) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(call.message, color = Text2)
            if(call.phase == "preparing") LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)
            if(call.phase == "connected") TextButton(onClick = { controller.toggleMute() }) { Text(if(call.muted) "取消静音" else "麦克风静音", color = Accent) }
        } },
        confirmButton = {
            when(call.phase) {
                "incoming" -> TextButton(onClick = { controller.accept() }) { Text("接听", color = Accent) }
                "ended", "error" -> TextButton(onClick = { controller.dismiss() }) { Text("关闭", color = Accent) }
                else -> TextButton(onClick = { controller.hangup() }) { Text("挂断", color = DangerText) }
            }
        },
        dismissButton = { if(call.phase == "incoming") TextButton(onClick = { controller.hangup() }) { Text("拒绝", color = DangerText) } }
    )
}

/** Local, fixed-origin engine only. No login cookies or tokens enter JavaScript. */
@SuppressLint("SetJavaScriptEnabled")
@Composable private fun VoiceAudioEngine(controller: SteamVoiceController, epoch: Int) {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val web = remember(epoch) { WebView(context).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isFocusable = false; isFocusableInTouchMode = false
        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        settings.javaScriptEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = false; settings.allowContentAccess = false
        settings.blockNetworkLoads = true
        webViewClient = object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
        }
        webChromeClient = object: WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if(request.origin.toString().trimEnd('/') == "https://appassets.androidplatform.net" &&
                    context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources) request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                else request.deny()
            }
        }
        addJavascriptInterface(object {
            @JavascriptInterface fun event(raw: String) { controller.browserEvent(epoch, raw) }
        }, "VoiceBridge")
    } }
    LaunchedEffect(web, epoch) {
        controller.commands.collect { raw ->
            if(JSONObject(raw).optInt("epoch") == epoch) web.evaluateJavascript("window.steamVoiceCommand($raw);", null)
        }
    }
    DisposableEffect(web) {
        val oldMode = manager.mode
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
        web.loadDataWithBaseURL("https://appassets.androidplatform.net/", context.assets.open("voice-engine.html").bufferedReader().use { it.readText() }, "text/html", "UTF-8", null)
        onDispose { web.removeJavascriptInterface("VoiceBridge"); web.stopLoading(); web.destroy(); manager.mode = oldMode }
    }
    // A root child receives the window's minimum constraints. Constrain inside a Box
    // so the audio-only WebView cannot grow to a full-screen white surface.
    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { web }, modifier = Modifier.size(1.dp).alpha(0f))
    }
}
