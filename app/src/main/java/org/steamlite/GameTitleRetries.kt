package org.steamlite

/** Three automatic attempts with backoff; a user refresh explicitly resets the budget. */
class GameTitleRetries {
    private data class Attempt(val count: Int, val next: Long)
    private val attempts = mutableMapOf<Int, Attempt>()
    @Synchronized fun ready(id: Int, now: Long): Boolean = attempts[id]?.let { it.count < 3 && now >= it.next } ?: true
    @Synchronized fun started(id: Int, now: Long) {
        val count = (attempts[id]?.count ?: 0) + 1
        attempts[id] = Attempt(count, now + if(count == 1) 30_000 else 120_000)
    }
    @Synchronized fun next(ids: Collection<Int>): Long? = ids.mapNotNull { attempts[it]?.takeIf { a -> a.count < 3 }?.next }.minOrNull()
    @Synchronized fun reset(ids: Collection<Int>) { ids.forEach { attempts.remove(it) } }
    @Synchronized fun clear() { attempts.clear() }
}
