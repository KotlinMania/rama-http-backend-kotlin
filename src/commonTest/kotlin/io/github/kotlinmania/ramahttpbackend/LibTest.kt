// port-lint: tests lib.rs
package io.github.kotlinmania.ramahttpbackend

import io.github.kotlinmania.ramahttpbackend.client.HttpClientService
import io.github.kotlinmania.ramahttpbackend.client.proxy.layer.HttpProxyAddressLayer
import io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector.HttpProxyConnector
import io.github.kotlinmania.ramahttpbackend.server.HttpServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibTest {

    @Test
    fun testHttp11Pipelining() {
        val server = HttpServer.http1()
        val client = HttpClientService.http1<Body>()

        val uri = Uri.fromString("http://127.0.0.1:8080/foo")
        val req = Request.builder()
            .method(Method.GET)
            .uri(uri)
            .version(Version.HTTP_11)
            .body(Body.empty())
            .unwrap()

        val resp = runSync { client.serve(req) }
        assertTrue(resp.isOk)
        assertEquals(StatusCode.OK, resp.unwrap().status)
    }

    @Test
    fun testHttp2Multiplex() {
        val server = HttpServer.h2()
        val client = HttpClientService.http2<Body>()

        val uri = Uri.fromString("https://127.0.0.1:8443/stream")
        val req = Request.builder()
            .method(Method.GET)
            .uri(uri)
            .version(Version.HTTP_2)
            .body(Body.empty())
            .unwrap()

        val resp = runSync { client.serve(req) }
        assertTrue(resp.isOk)
        assertEquals(StatusCode.OK, resp.unwrap().status)
    }

    @Test
    fun testProxyAddressParsingAndLayer() {
        val addr = ProxyAddress.parse("http://user:pass@127.0.0.1:8080")
        assertEquals("127.0.0.1", addr.address.host)
        assertEquals(8080, addr.address.port)
        assertEquals(NetProtocol.Http, addr.protocol)

        val layer = HttpProxyAddressLayer.new(addr)
        val connector = HttpProxyConnector.optional(Any())
        assertEquals(Version.HTTP_11, connector.version)
    }
}
