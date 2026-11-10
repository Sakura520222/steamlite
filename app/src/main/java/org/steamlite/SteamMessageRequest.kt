package org.steamlite

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Request
import java.util.UUID

/** Match the Steam client's 16 hexadecimal character message identifier, not a UUID string. */
internal fun privateMessageRequest(peer: Long, text: String): CFriendMessages_SendMessage_Request =
    CFriendMessages_SendMessage_Request.newBuilder()
        .setSteamid(peer)
        .setChatEntryType(1)
        .setMessage(ChatContent.outgoingMessage(text))
        .setContainsBbcode(text.contains('[') || text.contains('ː'))
        .setEchoToSender(false)
        .setClientMessageId(UUID.randomUUID().toString().replace("-", "").take(16))
        .build()
