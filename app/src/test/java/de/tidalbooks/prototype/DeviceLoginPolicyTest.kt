package de.tidalbooks.prototype

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceLoginPolicyTest {
    @Test fun successRequiresBothSuccessfulResponseAndToken() {
        assertEquals(DeviceLoginDecision.COMPLETE, deviceLoginDecision(200, true, "", 0))
        assertEquals(DeviceLoginDecision.FAIL, deviceLoginDecision(200, false, "", 0))
        assertEquals(DeviceLoginDecision.FAIL, deviceLoginDecision(403, true, "", 0))
    }
    @Test fun bothKnownPendingFormatsKeepWaiting() {
        assertEquals(DeviceLoginDecision.WAIT, deviceLoginDecision(400, false, "authorization_pending", 0))
        assertEquals(DeviceLoginDecision.WAIT, deviceLoginDecision(400, false, "", 1002))
    }
    @Test fun slowDownTakesPriorityOverPendingSubstatus() {
        assertEquals(DeviceLoginDecision.SLOW_DOWN, deviceLoginDecision(400, false, "slow_down", 1002))
        assertEquals(DeviceLoginDecision.SLOW_DOWN, deviceLoginDecision(429, false, "", 0))
    }
    @Test fun denialExpiryAndServerErrorsNeverBecomeALogin() {
        for ((status, error) in listOf(400 to "expired_token", 400 to "access_denied", 401 to "invalid_client",
            403 to "access_denied")) {
            assertEquals(DeviceLoginDecision.FAIL, deviceLoginDecision(status, false, error, 0))
        }
    }
    @Test fun transientServerErrorsKeepTheApprovalPendingWithoutAuthenticating() {
        assertEquals(DeviceLoginDecision.WAIT, deviceLoginDecision(500, false, "", 0))
        assertEquals(DeviceLoginDecision.WAIT, deviceLoginDecision(503, true, "", 0))
    }
}
