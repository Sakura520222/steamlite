package org.steamlite

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.steamlite.protocol.VoiceProtocol.*

data class SteamCall(val phase: String = "idle", val peer: Long = 0, val message: String = "", val epoch: Int = 0, val muted: Boolean = false)
class VoiceJoinGate {
    var ice = false
    var session = false
    var requested = false
    var accepted = false
    var bound = false
    fun canRequest() = ice && session && !requested
    fun canBind() = ice && session && accepted && !bound
}

/** Signalling contains no microphone data; the local WebView supplies the WebRTC audio engine. */
class SteamVoiceController(unified: SteamUnifiedMessages, manager: CallbackManager, private val scope: CoroutineScope,
    private val owner: () -> String, private val online: () -> Boolean) {
    val state = MutableStateFlow(SteamCall())
    val commands = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 16)
    private val voice = unified.createService<VoiceService>()
    private val rtc = unified.createService<VoiceRtcService>()
    private val lock = Mutex()
    private var account = ""
    private var gate = VoiceJoinGate()
    private var session: VoiceConnected? = null
    @Volatile private var voiceId = 0L
    private var incoming = false
    private var watchdog: Job? = null
    init {
        manager.subscribeServiceNotification<VoiceRtcNotifications, VoiceConnected.Builder> { e -> action {
            if(active()) { session = e.body.build(); gate.session = true; advance() }
        } }
        manager.subscribeServiceNotification<VoiceRtcNotifications, VoiceRemoteUpdate.Builder> { e -> action {
            if(active()) emit("remote", JSONObject().put("description", e.body.remoteDescription).put("version", e.body.remoteDescriptionVersion))
        } }
        manager.subscribeServiceNotification<VoiceNotifications, VoiceIncoming.Builder> { e -> action {
            if(online() && state.value.phase in setOf("idle", "ended", "error")) {
                reset(e.body.steamidPartner); incoming = true; voiceId = e.body.voiceChatid
                state.value = state.value.copy(phase = "incoming", message = "Steam 语音来电")
                deadline(60000)
            }
        } }
        manager.subscribeServiceNotification<VoiceNotifications, VoiceAccepted.Builder> { e -> action {
            if(active() && e.body.steamidPartner == state.value.peer && e.body.voicechatId == voiceId) {
                if(e.body.acceptedRequest) { gate.accepted = true; advance() } else finish("对方未接听", "ended")
            }
        } }
        manager.subscribeServiceNotification<VoiceNotifications, VoiceEnded.Builder> { e ->
            if(voiceId != 0L && e.body.voiceChatid == voiceId && state.value.phase !in setOf("idle", "ended", "error")) abortLocal("语音通话已结束")
        }
    }
    private fun active() = online() && owner() == account && state.value.phase in setOf("preparing", "ringing", "connecting", "connected")
    private fun reset(peer: Long) {
        watchdog?.cancel(); account = owner(); gate = VoiceJoinGate(); session = null; voiceId = 0; incoming = false
        state.value = SteamCall("permission", peer, "等待麦克风权限", state.value.epoch + 1)
    }
    fun request(peer: Long) = action {
        check(online() && peer > 0 && peer.toString() != owner()) { "请先登录并选择好友" }
        check(state.value.phase in setOf("idle", "ended", "error")) { "请先结束当前通话" }
        reset(peer)
    }
    fun accept() = action { if(state.value.phase == "incoming") state.value = state.value.copy(phase = "permission", message = "等待麦克风权限") }
    fun permission(granted: Boolean) = action {
        if(state.value.phase != "permission") return@action
        if(!granted) { terminate(); finish("未获得麦克风权限，未建立语音连接", "error"); return@action }
        check(online() && account == owner()) { "登录会话已改变" }
        val epoch = state.value.epoch
        if(incoming) {
            val result = withTimeout(15000) { voice.answer(voiceId, state.value.peer, true).await() }
            if(epoch != state.value.epoch || account != owner()) return@action
            check(result.result == EResult.OK) { "Steam 拒绝接听：${result.result}" }
            gate.accepted = true
        }
        state.value = state.value.copy(phase = "preparing", message = "正在连接 Steam 语音")
        deadline(35000)
    }
    fun browserEvent(epoch: Int, raw: String) = action {
        if(epoch != state.value.epoch || !active() || raw.length > 262144) return@action
        val event = JSONObject(raw)
        when(event.optString("op")) {
            "ready" -> emit("start")
            "offer" -> {
                val result = withTimeout(20000) { rtc.initiate(VoiceOffer.newBuilder().setSdp(event.getString("description")).setBrowserName("Chrome").setBrowserVersion(event.optString("version", "0")).build()).await() }
                if(epoch != state.value.epoch || !active()) return@action
                check(result.result == EResult.OK) { "Steam 拒绝语音连接：${result.result}" }
                emit("answer", JSONObject().put("description", result.body.remoteDescription))
            }
            "ice" -> { gate.ice = true; advance() }
            "renegotiated" -> {
                val s = session ?: return@action
                if(event.has("description")) {
                    val result = withTimeout(15000) { rtc.update(VoiceRenegotiate.newBuilder().setIpWebrtcServer(s.serverIp).setPortWebrtcServer(s.serverPort).setIpWebrtcSessionClient(s.clientIp).setPortWebrtcSessionClient(s.clientPort).setSdp(event.getString("description")).build()).await() }
                    if(epoch != state.value.epoch || !active()) return@action
                    check(result.result == EResult.OK) { "语音重新协商失败：${result.result}" }
                    if(result.body.remoteDescription.isNotBlank()) emit("answer", JSONObject().put("description", result.body.remoteDescription))
                }
                val result = withTimeout(15000) { rtc.ack(VoiceAck.newBuilder().setIpWebrtcServer(s.serverIp).setPortWebrtcServer(s.serverPort).setIpWebrtcSessionClient(s.clientIp).setPortWebrtcSessionClient(s.clientPort).setRemoteDescriptionVersion(event.getLong("version")).build()).await() }
                check(result.result == EResult.OK) { "语音协商确认失败：${result.result}" }
            }
            "failed" -> { terminate(); finish("语音音频连接失败，请检查麦克风和网络", "error") }
        }
    }
    private suspend fun advance() {
        if(!active()) return
        val epoch = state.value.epoch
        if(gate.canRequest()) {
            gate.requested = true
            if(!incoming) {
                val result = withTimeout(20000) { voice.call(state.value.peer).await() }
                if(epoch != state.value.epoch || !active()) return
                check(result.result == EResult.OK) { "Steam 拒绝呼叫：${result.result}" }
                voiceId = result.body.voiceChatid
                state.value = state.value.copy(phase = "ringing", message = "正在等待对方接听")
                deadline(60000)
            }
        }
        if(gate.canBind() && voiceId != 0L) {
            gate.bound = true
            val s = session ?: return
            val request = VoiceBind.newBuilder().setVoiceChatid(voiceId).setIpWebrtcServer(s.serverIp).setPortWebrtcServer(s.serverPort)
                .setIpWebrtcClient(s.clientIp).setPortWebrtcClient(s.clientPort).setSsrcMySendingStream(s.ssrc).setUserAgent("SteamLite Android WebView").setHasAudioWorkletsSupport(false).build()
            val result = withTimeout(15000) { voice.bind(request).await() }
            if(epoch != state.value.epoch || !active()) return
            check(result.result == EResult.OK) { "Steam 未接通音频：${result.result}" }
            watchdog?.cancel(); state.value = state.value.copy(phase = "connected", message = "语音通话中")
        }
    }
    fun toggleMute() = action { if(active()) { state.value = state.value.copy(muted = !state.value.muted); emit("mute", JSONObject().put("value", state.value.muted)) } }
    fun hangup() {
        if(state.value.phase in setOf("idle", "ended", "error")) return
        val peer = state.value.peer; val id = voiceId; val reject = incoming && !gate.accepted; val requested = gate.requested
        val accountOwner = account
        abortLocal("语音通话已结束")
        scope.launch { if(online() && owner() == accountOwner) terminateTarget(peer, id, reject, requested) }
    }
    fun dismiss() { if(state.value.phase in setOf("ended", "error")) state.value = state.value.copy(phase = "idle") }
    fun disconnect() { if(state.value.phase !in setOf("idle", "ended", "error")) abortLocal("Steam 连接已断开") }
    private fun abortLocal(message: String) {
        watchdog?.cancel()
        state.value = state.value.copy(phase = "ended", message = message, epoch = state.value.epoch + 1)
    }
    private suspend fun terminate() {
        if(!online() || account != owner()) return
        terminateTarget(state.value.peer, voiceId, incoming && !gate.accepted, gate.requested)
    }
    private suspend fun terminateTarget(peer: Long, id: Long, reject: Boolean, requested: Boolean) {
        if(reject && id != 0L) runCatching { withTimeout(5000) { voice.answer(id, peer, false).await() } }
        else {
            if(requested && peer != 0L) runCatching { withTimeout(5000) { voice.end(peer).await() } }
            if(id != 0L) runCatching { withTimeout(5000) { voice.leave(id).await() } }
        }
    }
    private suspend fun emit(op: String, params: JSONObject = JSONObject()) { commands.emit(params.put("op", op).put("epoch", state.value.epoch).toString()) }
    private fun deadline(ms: Long) { watchdog?.cancel(); val epoch = state.value.epoch; watchdog = scope.launch { delay(ms); action { if(state.value.epoch == epoch && state.value.phase !in setOf("idle", "ended", "error", "connected")) { terminate(); finish("语音连接或接听超时", "ended") } } } }
    private suspend fun finish(message: String, phase: String) { watchdog?.cancel(); emit("stop"); state.value = state.value.copy(phase = phase, message = message) }
    private fun action(block: suspend () -> Unit) { val epoch = state.value.epoch; scope.launch {
        try { lock.withLock { if(epoch == state.value.epoch) block() } } catch(e: CancellationException) { throw e }
        catch(e: Exception) { lock.withLock { if(epoch == state.value.epoch) { terminate(); finish(if(e is IllegalStateException) e.message ?: "语音连接失败" else "语音连接失败（${e.javaClass.simpleName}）", "error") } } }
    } }
}
