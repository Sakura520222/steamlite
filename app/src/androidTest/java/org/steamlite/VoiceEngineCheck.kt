package org.steamlite

import android.app.Instrumentation
import android.webkit.*
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Loads local engine only: no microphone request, network signalling or friend call. */
object VoiceEngineCheck {
    fun run(test: Instrumentation): String {
        val loaded = CountDownLatch(1)
        val permissionRequests = AtomicInteger()
        var answer = ""
        lateinit var web: WebView
        test.runOnMainSync {
            web = WebView(test.targetContext)
            web.settings.javaScriptEnabled = true
            web.settings.allowFileAccess = false; web.settings.allowContentAccess = false; web.settings.blockNetworkLoads = true
            web.addJavascriptInterface(object { @JavascriptInterface fun event(ignored: String) = Unit }, "VoiceBridge")
            web.webChromeClient = object: WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) { permissionRequests.incrementAndGet(); request.deny() }
            }
            web.webViewClient = object: WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    web.evaluateJavascript("({secure:isSecureContext,rtc:typeof RTCPeerConnection==='function',media:!!navigator.mediaDevices,engine:typeof steamVoiceCommand==='function'})") { value -> answer = value; loaded.countDown() }
                }
            }
            web.loadDataWithBaseURL("https://appassets.androidplatform.net/", test.targetContext.assets.open("voice-engine.html").bufferedReader().use { it.readText() }, "text/html", "UTF-8", null)
        }
        return try {
            check(loaded.await(15, TimeUnit.SECONDS)) { "engine load timeout" }
            val json = JSONObject(answer)
            check(listOf("secure", "rtc", "media", "engine").all { json.optBoolean(it) }) { "WebRTC capability unavailable" }
            check(permissionRequests.get() == 0) { "unexpected microphone request" }
            "PASS: local secure origin, WebRTC/media APIs, engine loaded, no microphone request"
        } finally { test.runOnMainSync { web.removeJavascriptInterface("VoiceBridge"); web.destroy() } }
    }
}
