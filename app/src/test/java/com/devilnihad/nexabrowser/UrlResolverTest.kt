package com.devilnihad.nexabrowser

import com.devilnihad.nexabrowser.utils.UrlResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlResolverTest {
    private val search = "https://www.google.com/search?q="

    @Test fun directUrlIsPreserved() {
        assertEquals("https://example.com", UrlResolver.resolve("https://example.com", search))
    }

    @Test fun domainGetsHttps() {
        assertEquals("https://example.com", UrlResolver.resolve("example.com", search))
    }

    @Test fun textBecomesSearch() {
        assertEquals(
            "https://www.google.com/search?q=hello%20world",
            UrlResolver.resolve("hello world", search)
        )
    }
}
