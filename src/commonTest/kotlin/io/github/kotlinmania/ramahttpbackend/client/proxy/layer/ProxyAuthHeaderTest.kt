package io.github.kotlinmania.ramahttpbackend.client.proxy.layer

import io.github.kotlinmania.ramahttpbackend.HostWithPort
import io.github.kotlinmania.ramahttpbackend.NetProtocol
import io.github.kotlinmania.ramahttpbackend.ProxyAddress
import io.github.kotlinmania.ramahttpbackend.ProxyCredential
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Response
import io.github.kotlinmania.ramahttpbackend.Service
import io.github.kotlinmania.ramahttpbackend.runSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ProxyAuthHeaderTest {
    @Test
    fun absentCredentialsPreserveExistingHeaderAndForwardRequest() {
        for (proxy in listOf(null, ProxyAddress(HostWithPort.exampleDomainHttp()))) {
            val request = Request()
            request.headers.insert("proxy-authorization", "existing")
            if (proxy != null) request.extensions.insert(proxy)
            assertEquals("existing", serve(request))
        }
    }

    @Test
    fun basicCredentialsAreAddedOnlyForAnInsecureProxy() {
        val credential = ProxyCredential.Basic("user", "password")
        for (protocol in listOf(NetProtocol.Http, NetProtocol.Https)) {
            val request = Request()
            request.extensions.insert(ProxyAddress(HostWithPort.exampleDomainHttp(), protocol, credential))
            assertEquals(if (protocol == NetProtocol.Http) credential.headerValue() else null, serve(request))
        }
    }

    @Test
    fun bearerCredentialsAreAddedForBothProxyProtocols() {
        val credential = ProxyCredential.Bearer("token")
        for (protocol in listOf(NetProtocol.Http, NetProtocol.Https)) {
            val request = Request()
            request.extensions.insert(ProxyAddress(HostWithPort.exampleDomainHttp(), protocol, credential))
            assertEquals("Bearer token", serve(request))
        }
    }

    private fun serve(request: Request): String? {
        val response = Response()
        val inner = object : Service<Request, Response, Throwable> {
            override suspend fun serve(input: Request): RamaResult<Response, Throwable> {
                assertSame(request, input)
                return RamaResult.Ok(response)
            }
        }
        val result = runSync { SetProxyAuthHttpHeaderService(inner).serve(request) }
        assertSame(response, result.unwrap())
        return request.headers.get("proxy-authorization")
    }
}
