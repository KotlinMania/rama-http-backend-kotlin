// port-lint: source client/proxy/layer/proxy_auth_header.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer

import io.github.kotlinmania.ramahttpbackend.Layer
import io.github.kotlinmania.ramahttpbackend.ProxyAddress
import io.github.kotlinmania.ramahttpbackend.ProxyCredential
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Response
import io.github.kotlinmania.ramahttpbackend.Service

/**
 * Proxy authorization header layer for Rama HTTP clients.
 */
internal class SetProxyAuthHttpHeaderLayer : Layer<Service<Request, Response, Throwable>, SetProxyAuthHttpHeaderService> {
    override fun layer(inner: Service<Request, Response, Throwable>): SetProxyAuthHttpHeaderService =
        SetProxyAuthHttpHeaderService(inner)

    override fun intoLayer(inner: Service<Request, Response, Throwable>): SetProxyAuthHttpHeaderService =
        layer(inner)

    public companion object {
        /**
         * Create a new [SetProxyAuthHttpHeaderLayer].
         */
        public fun new(): SetProxyAuthHttpHeaderLayer = SetProxyAuthHttpHeaderLayer()
    }
}

/**
 * Proxy authorization header service for Rama HTTP clients.
 */
internal class SetProxyAuthHttpHeaderService(
    public val inner: Service<Request, Response, Throwable>,
) : Service<Request, Response, Throwable> {
    /**
     * Mutable reference to inner service.
     */
    public fun innerMut(): Service<Request, Response, Throwable> = inner

    override suspend fun serve(input: Request): RamaResult<Response, Throwable> {
        val proxyAddress = input.extensions.get<ProxyAddress>()
        if (proxyAddress != null && proxyAddress.credential != null) {
            when (val cred = proxyAddress.credential) {
                is ProxyCredential.Basic -> {
                    val isSecure = proxyAddress.protocol?.isSecure() ?: false
                    if (!isSecure) {
                        input.headersMut().insert("proxy-authorization", cred.headerValue())
                    }
                }
                is ProxyCredential.Bearer -> {
                    input.headersMut().insert("proxy-authorization", cred.headerValue())
                }
            }
        }
        return inner.serve(input)
    }

    public companion object {
        /**
         * Create a new [SetProxyAuthHttpHeaderService].
         */
        public fun new(
            inner: Service<Request, Response, Throwable>,
        ): SetProxyAuthHttpHeaderService = SetProxyAuthHttpHeaderService(inner)
    }
}
