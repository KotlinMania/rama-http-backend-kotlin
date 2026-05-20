package io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class HttpProxyErrorTest {
    @Test
    fun authRequiredFormatsLikeUpstreamDisplay() {
        assertEquals("http proxy error: proxy auth required (http 407)", HttpProxyError.AuthRequired.message)
        assertEquals("http proxy error: proxy auth required (http 407)", HttpProxyError.AuthRequired.toString())
        assertNull(HttpProxyError.AuthRequired.cause)
    }

    @Test
    fun unavailableFormatsLikeUpstreamDisplay() {
        assertEquals("http proxy error: proxy unavailable (http 503)", HttpProxyError.Unavailable.message)
        assertEquals("http proxy error: proxy unavailable (http 503)", HttpProxyError.Unavailable.toString())
        assertNull(HttpProxyError.Unavailable.cause)
    }

    @Test
    fun transportFormatsAndExposesItsErrorSource() {
        val transportError = DescribedThrowable("tcp reset")
        val error = HttpProxyError.Transport(transportError)

        assertEquals("http proxy error: transport error: I/O [tcp reset]", error.message)
        assertEquals("http proxy error: transport error: I/O [tcp reset]", error.toString())
        assertSame(transportError, error.cause)
    }

    @Test
    fun transportUsesNestedCauseWhenAvailable() {
        val inner = DescribedThrowable("blocked address")
        val outer = WrappedThrowable("outer", inner)

        assertSame(inner, HttpProxyError.Transport(outer).cause)
    }

    @Test
    fun otherIncludesTheFirstHeaderLine() {
        val error = HttpProxyError.Other("HTTP/1.1 418 I'm a teapot")

        assertEquals(
            "http proxy error: first line of header = [HTTP/1.1 418 I'm a teapot]",
            error.message,
        )
        assertEquals(
            "http proxy error: first line of header = [HTTP/1.1 418 I'm a teapot]",
            error.toString(),
        )
        assertNull(error.cause)
    }

    private class DescribedThrowable(private val description: String) : Throwable(description) {
        override fun toString(): String = description
    }

    private class WrappedThrowable(
        private val description: String,
        private val nestedCause: Throwable,
    ) : Throwable(description) {
        override val cause: Throwable?
            get() = nestedCause

        override fun toString(): String = description
    }
}
