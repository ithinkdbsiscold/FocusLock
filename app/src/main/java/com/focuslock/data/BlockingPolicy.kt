package com.focuslock.data

/** Deterministic rules kept separate from Android APIs for reliable testing. */
object BlockingPolicy {
    /** Once a day is blocked, it remains blocked until a new accounting-date record is used. */
    fun shouldBlock(blockingEnabled: Boolean, alreadyBlocked: Boolean, usageMillis: Long, limitMillis: Long): Boolean =
        blockingEnabled && (alreadyBlocked || usageMillis >= limitMillis.coerceAtLeast(1L))

    fun mayChangeRestrictions(strictMode: Boolean): Boolean = !strictMode
}
