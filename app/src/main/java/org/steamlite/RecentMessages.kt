package org.steamlite

internal fun recordRecentMessage(rows: List<RecentSession>, peer: Long, time: Long, unread: Boolean): List<RecentSession> {
    val previous = rows.firstOrNull { it.peer == peer }
    return (rows.filterNot { it.peer == peer } + RecentSession(peer, maxOf(time, previous?.time ?: 0), (previous?.unread ?: 0) + if (unread) 1 else 0)).sortedByDescending { it.time }
}

internal fun mergeRecentMessages(local: List<RecentSession>, server: List<RecentSession>, readTimes: Map<Long, Long>): List<RecentSession> =
    (server + local.filter { row -> server.none { it.peer == row.peer && it.time >= row.time } })
        .groupBy { it.peer }.map { (_, rows) -> rows.maxBy { it.time } }
        .map { row -> if (row.time <= (readTimes[row.peer] ?: -1)) row.copy(unread = 0) else row }
        .sortedByDescending { it.time }
