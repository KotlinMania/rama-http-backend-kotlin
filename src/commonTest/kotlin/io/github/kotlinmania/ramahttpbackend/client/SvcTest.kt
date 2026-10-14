// port-lint: tests client/svc.rs
package io.github.kotlinmania.ramahttpbackend.client

import io.github.kotlinmania.ramahttp.types.Authority
import io.github.kotlinmania.ramahttp.types.Body
import io.github.kotlinmania.ramahttp.types.Method
import io.github.kotlinmania.ramahttp.types.Request
import io.github.kotlinmania.ramahttp.types.Scheme
import io.github.kotlinmania.ramahttp.types.Uri
import io.github.kotlinmania.ramanet.NetProtocol
import io.github.kotlinmania.ramanet.address.HostWithPort
import io.github.kotlinmania.ramanet.address.ProxyAddress
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

        val sanitized = sanitizeClientReqHeader(req).unwrap()
        val (parts, _) = sanitized.intoParts()
        val uriParts = parts.uri.intoParts()

        assertEquals(Scheme("https"), uriParts.scheme)
        assertEquals(Authority("example.com"), uriParts.authority)
    }

    @Test
    fun shouldNotSanitizeInsecureHttp1RequestOverHttpProxy() {
        val uri = Uri.fromString("http://example.com/test")
        val req =
            Request
                .builder()
                .uri(uri)
                .body(Body.empty())

        req.extensions.insert(
            ProxyAddress(
                address = HostWithPort.exampleDomainHttp(),
                credential = null,
                protocol = NetProtocol.Http,
            ),
        )

        val sanitized = sanitizeClientReqHeader(req).unwrap()
        val (parts, _) = sanitized.intoParts()
        val uriParts = parts.uri.intoParts()

        assertEquals(Scheme("http"), uriParts.scheme)
        assertEquals(Authority("example.com"), uriParts.authority)
    }

    @Test
    fun shouldSanitizeSecureHttp1RequestOverHttpProxy() {
        val uri = Uri.fromString("https://example.com/test")
        val req =
            Request
                .builder()
                .uri(uri)
                .body(Body.empty())

        req.extensions.insert(
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

        req.extensions.insert(
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
