package org.steamlite

// Group identity must survive title loading, localization and temporary network failures.
fun friendGameKey(friend: FriendRow): String = if(friend.gameAppId > 0) "app:${friend.gameAppId}" else "custom:${friend.game}"
