package org.steamlite

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler

class StickerHandler(private val received: (List<String>) -> Unit): ClientMsgHandler() {
    override fun handleMsg(packetMsg: IPacketMsg) {
        if(packetMsg.msgType != EMsg.ClientEmoticonList) return
        val body = ClientMsgProtobuf<CMsgClientEmoticonList.Builder>(CMsgClientEmoticonList::class.java, packetMsg).body
        received(body.stickersList.map { it.name }.filter { it.isNotBlank() && it.none { c -> c in "\"[]\r\n" } }.distinct())
    }
}
