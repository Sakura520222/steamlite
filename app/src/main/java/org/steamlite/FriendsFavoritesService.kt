package org.steamlite

import `in`.dragonbra.javasteam.base.PacketClientMsgProtobuf
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.UnifiedService
import org.steamlite.protocol.FriendsProtocol.*

class FriendsFavoritesService(unified: SteamUnifiedMessages): UnifiedService(unified) {
    override val serviceName = "FriendsList"
    override fun handleNotificationMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) = Unit
    override fun handleResponseMsg(methodName: String, packetMsg: PacketClientMsgProtobuf) {
        when(methodName) {
            "GetFavorites" -> postResponseMsg<GetFavoritesResponse.Builder>(GetFavoritesResponse::class.java, packetMsg)
            "SetFavorites" -> postResponseMsg<SetFavoritesResponse.Builder>(SetFavoritesResponse::class.java, packetMsg)
        }
    }
    fun get() = unifiedMessages!!.sendMessage<GetFavoritesRequest.Builder, GetFavoritesResponse.Builder>(GetFavoritesResponse.Builder::class.java, "FriendsList.GetFavorites#1", GetFavoritesRequest.getDefaultInstance())
    fun set(entries: List<Favorite>) = unifiedMessages!!.sendMessage<SetFavoritesRequest.Builder, SetFavoritesResponse.Builder>(SetFavoritesResponse.Builder::class.java, "FriendsList.SetFavorites#1", SetFavoritesRequest.newBuilder().addAllFavorites(entries).build())
}
