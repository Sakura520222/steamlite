package org.steamlite

import `in`.dragonbra.javasteam.base.PacketClientMsgProtobuf
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.UnifiedService
import org.steamlite.protocol.WishProtocol.*

/** Steam unified RPC; requests share the existing authenticated CM connection. */
class WishlistService(unified: SteamUnifiedMessages) : UnifiedService(unified) {
    override val serviceName = "Wishlist"
    override fun handleNotificationMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) = Unit
    override fun handleResponseMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) {
        when(methodName) {
            "GetWishlist" -> postResponseMsg<GetResponse.Builder>(GetResponse::class.java, packetMsg)
            "AddToWishlist" -> postResponseMsg<AddResponse.Builder>(AddResponse::class.java, packetMsg)
            "RemoveFromWishlist" -> postResponseMsg<RemoveResponse.Builder>(RemoveResponse::class.java, packetMsg)
        }
    }
    fun get(steamId: Long) = unifiedMessages!!.sendMessage<GetRequest.Builder, GetResponse.Builder>(GetResponse.Builder::class.java, "Wishlist.GetWishlist#1", GetRequest.newBuilder().setSteamid(steamId).build())
    fun add(appId: Int) = unifiedMessages!!.sendMessage<AddRequest.Builder, AddResponse.Builder>(AddResponse.Builder::class.java, "Wishlist.AddToWishlist#1", AddRequest.newBuilder().setAppid(appId).build())
    fun remove(appId: Int) = unifiedMessages!!.sendMessage<RemoveRequest.Builder, RemoveResponse.Builder>(RemoveResponse.Builder::class.java, "Wishlist.RemoveFromWishlist#1", RemoveRequest.newBuilder().setAppid(appId).build())
}
