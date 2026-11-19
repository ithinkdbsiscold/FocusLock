package com.focuslock.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingPolicyTest {
    @Test fun appStaysAllowedBelowItsOwnLimit() {
        assertFalse(BlockingPolicy.shouldBlock(true, false, usageMillis = 29 * 60_000, limitMillis = 30 * 60_000))
    }

    @Test fun appBlocksAtItsOwnLimit() {
        assertTrue(BlockingPolicy.shouldBlock(true, false, usageMillis = 30 * 60_000, limitMillis = 30 * 60_000))
    }

    @Test fun aPersistedBlockSurvivesUsageReconciliationAndReboot() {
        assertTrue(BlockingPolicy.shouldBlock(true, alreadyBlocked = true, usageMillis = 1, limitMillis = 30 * 60_000))
    }

    @Test fun disabledRestrictionNeverBlocks() {
        assertFalse(BlockingPolicy.shouldBlock(false, alreadyBlocked = true, usageMillis = Long.MAX_VALUE, limitMillis = 1))
    }

    @Test fun strictModePreventsNormalChanges() {
        assertFalse(BlockingPolicy.mayChangeRestrictions(strictMode = true))
        assertTrue(BlockingPolicy.mayChangeRestrictions(strictMode = false))
    }
}
