// port-lint: tests rama-http-backend/src/client/svc.rs
package io.github.kotlinmania.ramahttpbackend.client

import io.github.kotlinmania.ramahttpbackend.Body
import io.github.kotlinmania.ramahttpbackend.HostWithPort
import io.github.kotlinmania.ramahttpbackend.Method
import io.github.kotlinmania.ramahttpbackend.NetProtocol
import io.github.kotlinmania.ramahttpbackend.ProxyAddress
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SvcTest {
    @Test
    fun shouldSanitizeHttp1ExceptConnect() {
        val methods =
            listOf(
                Method.DELETE,
                Method.GET,
                Method.HEAD,
                Method.OPTIONS,
                Method.PATCH,
                Method.POST,
                Method.PUT,
                Method.TRACE,
            )

        for (method in methods) {
            val uri = Uri.fromString("https://example.com/test")
            val req =
                Request
                    .builder()
                    .uri(uri)
                    .method(method)
                    .body(Body.empty())
                    .unwrap()

            val sanitized = sanitizeClientReqHeader(req).unwrap()
            val (parts, _) = sanitized.intoParts()
            val uriParts = parts.uri.intoParts()

            assertNull(uriParts.scheme)
            assertNull(uriParts.authority)
        }
    }

    @Test
    fun shouldNotSanitizeHttp1Connect() {
        val uri = Uri.fromString("https://example.com/test")
        val req =
            Request
                .builder()
                .method(Method.CONNECT)
                .uri(uri)
                .body(Body.empty())
                .unwrap()

        val sanitized = sanitizeClientReqHeader(req).unwrap()
        val (parts, _) = sanitized.intoParts()
        val uriParts = parts.uri.intoParts()

        assertEquals("https", uriParts.scheme)
        assertEquals("example.com", uriParts.authority)
    }

    @Test
    fun shouldNotSanitizeInsecureHttp1RequestOverHttpProxy() {
        val uri = Uri.fromString("http://example.com/test")
        val req =
            Request
                .builder()
                .uri(uri)
                .body(Body.empty())
                .unwrap()

        req.extensionsMut().insert(
            ProxyAddress(
                address = HostWithPort.exampleDomainHttp(),
                credential = null,
                protocol = NetProtocol.Http,
            ),
        )

        val sanitized = sanitizeClientReqHeader(req).unwrap()
        val (parts, _) = sanitized.intoParts()
        val uriParts = parts.uri.intoParts()

        assertEquals("http", uriParts.scheme)
        assertEquals("example.com", uriParts.authority)
    }

    @Test
    fun shouldSanitizeSecureHttp1RequestOverHttpProxy() {
        val uri = Uri.fromString("https://example.com/test")
        val req =
            Request
                .builder()
                .uri(uri)
                .body(Body.empty())
                .unwrap()

        req.extensionsMut().insert(
            ProxyAddress(
                address = HostWithPort.exampleDomainHttp(),
                credential = null,
                protocol = NetProtocol.Http,
            ),
        )

        val sanitized = sanitizeClientReqHeader(req).unwrap()
        val (parts, _) = sanitized.intoParts()
        val uriParts = parts.uri.intoParts()

        assertNull(uriParts.scheme)
        assertNull(uriParts.authority)
    }

    @Test
    fun shouldSanitizeInsecureHttp1RequestOverSocksProxy() {
        val uri = Uri.fromString("http://example.com/test")
        val req =
            Request
                .builder()
                .uri(uri)
                .body(Body.empty())
                .unwrap()

        req.extensionsMut().insert(
            ProxyAddress(
                address = HostWithPort.exampleDomainHttp(),
                credential = null,
                protocol = NetProtocol.Socks5,
            ),
        )

        val sanitized = sanitizeClientReqHeader(req).unwrap()
        val (parts, _) = sanitized.intoParts()
        val uriParts = parts.uri.intoParts()

        assertNull(uriParts.scheme)
        assertNull(uriParts.authority)
    }
}
