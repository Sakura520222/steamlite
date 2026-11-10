package org.steamlite

import `in`.dragonbra.javasteam.base.PacketClientMsgProtobuf
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.*
import org.steamlite.protocol.VoiceProtocol.*

class VoiceService(unified: SteamUnifiedMessages): UnifiedService(unified) {
    override val serviceName = "VoiceChat"
    override fun handleNotificationMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) = Unit
    override fun handleResponseMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) {
        if(methodName == "RequestOneOnOneChat") postResponseMsg<VoiceId.Builder>(VoiceId::class.java, packetMsg)
        else postResponseMsg<EmptyVoice.Builder>(EmptyVoice::class.java, packetMsg)
    }
    fun call(peer: Long) = unifiedMessages!!.sendMessage<VoicePeer.Builder, VoiceId.Builder>(VoiceId.Builder::class.java, "VoiceChat.RequestOneOnOneChat#1", VoicePeer.newBuilder().setSteamidPartner(peer).build())
    fun answer(id: Long, peer: Long, accept: Boolean) = unifiedMessages!!.sendMessage<VoiceAnswer.Builder, EmptyVoice.Builder>(EmptyVoice.Builder::class.java, "VoiceChat.AnswerOneOnOneChat#1", VoiceAnswer.newBuilder().setVoiceChatid(id).setSteamidPartner(peer).setAcceptedRequest(accept).build())
    fun end(peer: Long) = unifiedMessages!!.sendMessage<VoicePeer.Builder, EmptyVoice.Builder>(EmptyVoice.Builder::class.java, "VoiceChat.EndOneOnOneChat#1", VoicePeer.newBuilder().setSteamidPartner(peer).build())
    fun leave(id: Long) = unifiedMessages!!.sendMessage<VoiceId.Builder, EmptyVoice.Builder>(EmptyVoice.Builder::class.java, "VoiceChat.LeaveVoiceChat#1", VoiceId.newBuilder().setVoiceChatid(id).build())
    fun bind(request: VoiceBind) = unifiedMessages!!.sendMessage<VoiceBind.Builder, EmptyVoice.Builder>(EmptyVoice.Builder::class.java, "VoiceChat.UpdateVoiceChatWebRTCData#1", request)
}
class VoiceRtcService(unified: SteamUnifiedMessages): UnifiedService(unified) {
    override val serviceName = "WebRTCClient"
    override fun handleNotificationMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) = Unit
    override fun handleResponseMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) {
        if(methodName == "AcknowledgeUpdatedRemoteDescription") postResponseMsg<EmptyVoice.Builder>(EmptyVoice::class.java, packetMsg)
        else postResponseMsg<RemoteVoice.Builder>(RemoteVoice::class.java, packetMsg)
    }
    fun initiate(request: VoiceOffer) = unifiedMessages!!.sendMessage<VoiceOffer.Builder, RemoteVoice.Builder>(RemoteVoice.Builder::class.java, "WebRTCClient.InitiateWebRTCConnection#1", request)
    fun update(request: VoiceRenegotiate) = unifiedMessages!!.sendMessage<VoiceRenegotiate.Builder, RemoteVoice.Builder>(RemoteVoice.Builder::class.java, "WebRTCClient.UpdateWebRTCConnection#1", request)
    fun ack(request: VoiceAck) = unifiedMessages!!.sendMessage<VoiceAck.Builder, EmptyVoice.Builder>(EmptyVoice.Builder::class.java, "WebRTCClient.AcknowledgeUpdatedRemoteDescription#1", request)
}
class VoiceRtcNotifications(unified: SteamUnifiedMessages): UnifiedService(unified) {
    override val serviceName = "WebRTCClientNotifications"
    override fun handleResponseMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) = Unit
    override fun handleNotificationMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) {
        when(methodName) {
            "NotifyWebRTCSessionConnected" -> postNotificationMsg<VoiceConnected.Builder>(VoiceConnected::class.java, packetMsg)
            "NotifyWebRTCUpdateRemoteDescription" -> postNotificationMsg<VoiceRemoteUpdate.Builder>(VoiceRemoteUpdate::class.java, packetMsg)
        }
    }
}
class VoiceNotifications(unified: SteamUnifiedMessages): UnifiedService(unified) {
    override val serviceName = "VoiceChatClient"
    override fun handleResponseMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) = Unit
    override fun handleNotificationMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) {
        when(methodName) {
            "NotifyOneOnOneChatRequested" -> postNotificationMsg<VoiceIncoming.Builder>(VoiceIncoming::class.java, packetMsg)
            "NotifyOneOnOneChatResponse" -> postNotificationMsg<VoiceAccepted.Builder>(VoiceAccepted::class.java, packetMsg)
            "NotifyVoiceChatEnded" -> postNotificationMsg<VoiceEnded.Builder>(VoiceEnded::class.java, packetMsg)
        }
    }
}
