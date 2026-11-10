package org.steamlite

import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.base.IPacketMsg
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientPersonaState
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler
import java.util.concurrent.ConcurrentHashMap

data class PersonaSnapshot(val state: Int? = null, val game: String = "", val gameId: Long = 0) {
    fun merge(state: Int?, game: String?, gameId: Long?): PersonaSnapshot {
        val nextState = state ?: this.state
        val nextId = gameId ?: this.gameId
        val ended = nextState == 0 || gameId == 0L
        return PersonaSnapshot(nextState, if(ended) "" else game ?: this.game, if(ended) 0 else nextId)
    }
}
/** Read field presence directly: omitted protobuf fields are not offline/empty updates. */
class PersonaHandler(private val changed: () -> Unit): ClientMsgHandler() {
    val snapshots = ConcurrentHashMap<Long, PersonaSnapshot>()
    override fun handleMsg(packetMsg: IPacketMsg) {
        if(packetMsg.msgType != EMsg.ClientPersonaState) return
        val body = ClientMsgProtobuf<CMsgClientPersonaState.Builder>(CMsgClientPersonaState::class.java, packetMsg).body
        body.friendsList.forEach { f ->
            snapshots.compute(f.friendid) { _, before -> (before ?: PersonaSnapshot()).merge(
                if(f.hasPersonaState()) f.personaState else null,
                if(f.hasGameName()) f.gameName else null,
                if(f.hasGameid()) f.gameid else if(f.hasGamePlayedAppId()) Integer.toUnsignedLong(f.gamePlayedAppId) else null
            ) }
        }
        changed()
    }
}
