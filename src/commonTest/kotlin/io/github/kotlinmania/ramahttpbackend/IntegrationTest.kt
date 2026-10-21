package io.github.kotlinmania.ramahttpbackend

import io.github.kotlinmania.http.StatusCode
import io.github.kotlinmania.ramahttp.types.Body
import io.github.kotlinmania.ramahttp.types.Method
import io.github.kotlinmania.ramahttp.types.Request
import io.github.kotlinmania.ramahttp.types.Uri
import io.github.kotlinmania.ramahttp.types.Version
import io.github.kotlinmania.ramahttpbackend.client.HttpClientService
import io.github.kotlinmania.ramahttpbackend.client.proxy.layer.HttpProxyAddressLayer
import io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector.HttpProxyConnector
import io.github.kotlinmania.ramahttpbackend.server.HttpServer
import io.github.kotlinmania.ramanet.NetProtocol
import io.github.kotlinmania.ramanet.address.ProxyAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntegrationTest {
    @Test
    fun testHttp11Pipelining() {
        val server = HttpServer.http1()
        assertNotNull(server)
        val client = HttpClientService.http1<Body>()

        val uri = Uri.fromString("http://127.0.0.1:8080/foo")
        val req =
            Request
                .builder()
                .method(Method.GET)
                .uri(uri)
                .version(Version.HTTP_11)
                .body(Body.empty())

        val resp = runSync { client.serve(req) }
        assertTrue(resp.isSuccess())
        assertEquals(StatusCode.OK, resp.value!!.status)
    }

    @Test
    fun testHttp2Multiplex() {
        val server = HttpServer.h2()
        assertNotNull(server)
        val client = HttpClientService.http2<Body>()

        val uri = Uri.fromString("https://127.0.0.1:8443/stream")
        val req =
            Request
                .builder()
                .method(Method.GET)
                .uri(uri)
                .version(Version.HTTP_2)
                .body(Body.empty())

        val resp = runSync { client.serve(req) }
        assertTrue(resp.isSuccess())
        assertEquals(StatusCode.OK, resp.value!!.status)
    }

    @Test
    fun testProxyAddressParsingAndLayer() {
        val addr = ProxyAddress.parse("http://user:pass@127.0.0.1:8080")
        assertEquals("127.0.0.1", addr.address.host.toString())
        assertEquals(8080.toUShort(), addr.address.port)
        assertEquals(NetProtocol.Http, addr.protocol)

        val layer = HttpProxyAddressLayer.new(addr)
        assertNotNull(layer)
        val connector = HttpProxyConnector.optional(Any())
        assertEquals(Version.HTTP_11, connector.version)
    }
}
