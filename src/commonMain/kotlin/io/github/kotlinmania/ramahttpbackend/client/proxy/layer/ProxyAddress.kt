// port-lint: source rama-http-backend/src/client/proxy/layer/proxy_address.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer

import io.github.kotlinmania.ramahttpbackend.Layer
import io.github.kotlinmania.ramahttpbackend.ProxyAddress
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Response
import io.github.kotlinmania.ramahttpbackend.Service

/**
 * Proxy address layer for Rama HTTP clients.
 */
internal class HttpProxyAddressLayer(
    public val address: ProxyAddress? = null,
    private var isPreserve: Boolean = false,
) : Layer<Service<Request, Response, Throwable>, HttpProxyAddressService> {

    public val preserve: Boolean get() = isPreserve

    /**
     * Set preserve flag.
     */
    public fun preserve(preserve: Boolean): HttpProxyAddressLayer {
        this.isPreserve = preserve
        return this
    }

    /**
     * Fluent alias for [preserve].
     */
    public fun withPreserve(preserve: Boolean): HttpProxyAddressLayer = preserve(preserve)

    /**
     * Mutate the preserve flag.
     */
    public fun setPreserve(preserve: Boolean) {
        this.isPreserve = preserve
    }

    override fun layer(inner: Service<Request, Response, Throwable>): HttpProxyAddressService =
        HttpProxyAddressService(inner, address, isPreserve)

    override fun intoLayer(inner: Service<Request, Response, Throwable>): HttpProxyAddressService =
        layer(inner)

    public companion object {
        /**
         * Create a new [HttpProxyAddressLayer] with the given proxy address.
         */
        public fun new(address: ProxyAddress): HttpProxyAddressLayer =
            HttpProxyAddressLayer(address, false)

        /**
         * Create a new [HttpProxyAddressLayer] with an optional proxy address.
         */
        public fun maybe(address: ProxyAddress?): HttpProxyAddressLayer =
            HttpProxyAddressLayer(address, false)

        /**
         * Try to create from default environment variable HTTP_PROXY.
         */
        public fun tryFromEnvDefault(): RamaResult<HttpProxyAddressLayer, Throwable> =
            tryFromEnv("HTTP_PROXY")

        /**
         * Try to create from environment variable key.
         */
        public fun tryFromEnv(key: String): RamaResult<HttpProxyAddressLayer, Throwable> =
            RamaResult.ok(maybe(null))
    }
}

/**
 * Proxy address service for Rama HTTP clients.
 */
internal class HttpProxyAddressService(
    public val inner: Service<Request, Response, Throwable>,
    public val proxyInfo: ProxyAddress? = null,
    private var isPreserve: Boolean = false,
) : Service<Request, Response, Throwable> {

    public val preserve: Boolean get() = isPreserve

    /**
     * Set preserve flag.
     */
    public fun preserve(preserve: Boolean): HttpProxyAddressService {
        this.isPreserve = preserve
        return this
    }

    /**
     * Fluent alias for [preserve].
     */
    public fun withPreserve(preserve: Boolean): HttpProxyAddressService = preserve(preserve)

    /**
     * Mutate preserve flag.
     */
    public fun setPreserve(preserve: Boolean) {
        this.isPreserve = preserve
    }

    /**
     * Mutable reference to inner service.
     */
    public fun innerMut(): Service<Request, Response, Throwable> = inner

    override suspend fun serve(input: Request): RamaResult<Response, Throwable> {
        if (proxyInfo != null) {
            if (!isPreserve || !input.extensions.contains<ProxyAddress>()) {
                input.extensionsMut().insert(proxyInfo)
            }
        }
        return inner.serve(input)
    }

    public companion object {
        /**
         * Create a new [HttpProxyAddressService] with the given proxy address.
         */
        public fun new(
            inner: Service<Request, Response, Throwable>,
            address: ProxyAddress,
        ): HttpProxyAddressService = HttpProxyAddressService(inner, address, false)

        /**
         * Create a new [HttpProxyAddressService] with an optional proxy address.
         */
        public fun maybe(
            inner: Service<Request, Response, Throwable>,
            address: ProxyAddress?,
        ): HttpProxyAddressService = HttpProxyAddressService(inner, address, false)

        /**
         * Try to create from default environment variable HTTP_PROXY.
         */
        public fun tryFromEnvDefault(
            inner: Service<Request, Response, Throwable>,
        ): RamaResult<HttpProxyAddressService, Throwable> = tryFromEnv(inner, "HTTP_PROXY")

        /**
         * Try to create from environment variable key.
         */
        public fun tryFromEnv(
            inner: Service<Request, Response, Throwable>,
            key: String,
        ): RamaResult<HttpProxyAddressService, Throwable> = RamaResult.ok(maybe(inner, null))
    }
}
