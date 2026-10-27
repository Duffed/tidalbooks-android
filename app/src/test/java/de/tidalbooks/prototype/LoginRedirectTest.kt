package de.tidalbooks.prototype

import org.junit.Assert.*
import org.junit.Test

class LoginRedirectTest {
    /** Exercise the parser shipped in the pinned SDK, without calling the network or Android. */
    private fun sdkParse(value: String): Any {
        val type = Class.forName("com.tidal.sdk.auth.login.RedirectUri")
        val companion = type.getField("Companion").get(null)
        return requireNotNull(companion.javaClass.getMethod("fromUriString", String::class.java).invoke(companion, value))
    }

    @Test fun callbackPreservesTheBaseUriRequiredForTokenExchange() {
        val callback = "tidalbooks://oauth/callback?code=test-code&state=test-state"
        var forwarded: String? = null
        assertTrue(dispatchLoginRedirect(callback) { forwarded = it })
        val parsed = sdkParse(requireNotNull(forwarded))
        assertEquals("Success", parsed.javaClass.simpleName)
        assertEquals("tidalbooks://oauth/callback", parsed.javaClass.getMethod("getUrl").invoke(parsed))
        assertEquals("test-code", parsed.javaClass.getMethod("getCode").invoke(parsed))
    }

    @Test fun bareQueryWouldProduceTheWrongSdkRedirectUriAndIsRejected() {
        val query = "code=test-code&state=test-state"
        val parsed = sdkParse(query)
        assertNotEquals("tidalbooks://oauth/callback", parsed.javaClass.getMethod("getUrl").invoke(parsed))
        assertFalse(dispatchLoginRedirect(query) { fail("A bare query must never reach finalizeLogin") })
    }

    @Test fun callbackQueryIsForwardedWithoutDecodingOrReordering() {
        val callback = "tidalbooks://oauth/callback?state=a%2Bb%3D&code=encoded%2Bcode%3D&extra=value"
        var forwarded: String? = null
        assertTrue(dispatchLoginRedirect(callback) { forwarded = it })
        assertEquals(callback, forwarded)
    }

    @Test fun oauthErrorsAreStillForwardedToSdkAsCompleteUris() {
        val callback = "tidalbooks://oauth/callback?error=access_denied&error_description=Cancelled"
        var forwarded: String? = null
        assertTrue(dispatchLoginRedirect(callback) { forwarded = it })
        assertEquals("Failure", sdkParse(requireNotNull(forwarded)).javaClass.simpleName)
    }

    @Test fun unrelatedMalformedOrEmptyCallbacksAreNotDispatched() {
        for (value in listOf("tidalbooks://other/callback?code=x", "tidalbooks://oauth/wrong?code=x",
            "https://oauth/callback?code=x", "tidalbooks://oauth/callback", "tidalbooks://oauth/callback?",
            "tidalbooks://user@oauth/callback?code=x", "tidalbooks://oauth:80/callback?code=x",
            "tidalbooks://oauth/callback?code=x#fragment", "not a uri")) {
            assertFalse(dispatchLoginRedirect(value) { fail("Unrelated callback was dispatched") })
        }
    }
}
