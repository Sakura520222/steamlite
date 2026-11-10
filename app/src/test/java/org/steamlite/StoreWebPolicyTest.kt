package org.steamlite

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class StoreWebPolicyTest {
    private val account = "https://store.steampowered.com/account/".toHttpUrl()
    @Test fun sameOriginRedirectsWork() {
        assertEquals("/account/", StoreWebPolicy.redirect(account, "/account/?l=english").encodedPath)
        assertEquals("store.steampowered.com", StoreWebPolicy.redirect(account, "https://store.steampowered.com/account/").host)
    }
    @Test fun credentialRequestsRejectUnsafeRedirects() {
        listOf("https://example.com/", "//example.com/", "http://store.steampowered.com/account/", "https://store.steampowered.com.example.com/", "https://store.steampowered.com:444/", "https://user:password@store.steampowered.com/").forEach { target ->
            assertThrows(IllegalArgumentException::class.java) { StoreWebPolicy.redirect(account, target) }
        }
    }
    @Test fun bothMutationFormsMatchCurrentCookieSession() {
        // Dummy cookie values only; these tests send no network requests.
        listOf("addtowishlist", "removefromwishlist").forEach { action ->
            val cookies = mapOf("sessionid" to "updated-session-from-server", "steamLoginSecure" to "unit-test-placeholder")
            val request = StoreWebPolicy.writeRequest("https://store.steampowered.com/api/$action".toHttpUrl(), 34900, cookies, "UnitTest")
            val form = request.body as FormBody
            val values = (0 until form.size).associate { form.name(it) to form.value(it) }
            assertEquals("34900", values["appid"])
            assertEquals(cookies["sessionid"], values["sessionid"])
            assertTrue(request.header("Cookie")!!.split("; ").contains("sessionid=${values["sessionid"]}"))
            assertEquals("POST", request.method)
        }
    }
    @Test fun missingCsrfSessionFailsBeforeRequestCreation() {
        assertThrows(IllegalStateException::class.java) { StoreWebPolicy.writeRequest("https://store.steampowered.com/api/addtowishlist".toHttpUrl(), 34900, emptyMap(), "UnitTest") }
    }
    @Test fun mutationBuilderAlsoRejectsExternalOrigins() {
        assertThrows(IllegalArgumentException::class.java) { StoreWebPolicy.writeRequest("https://example.com/api/addtowishlist".toHttpUrl(), 34900, mapOf("sessionid" to "dummy"), "UnitTest") }
    }
}
