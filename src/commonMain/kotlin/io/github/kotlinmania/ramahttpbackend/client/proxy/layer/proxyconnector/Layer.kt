// port-lint: source rama-http-backend/src/client/proxy/layer/proxy_connector/layer.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector

import io.github.kotlinmania.ramahttpbackend.HeaderMap
import io.github.kotlinmania.ramahttpbackend.Layer
import io.github.kotlinmania.ramahttpbackend.Version

/**
 * A [Layer] that wraps an inner connector with [HttpProxyConnector].
 */
public class HttpProxyConnectorLayer(
    public val required: Boolean = false,
    private var customVersion: Version? = null,
    private var customHeaders: HeaderMap? = null,
) : Layer<Any, HttpProxyConnector> {
    public val version: Version? get() = customVersion
    public val headers: HeaderMap? get() = customHeaders

    /**
     * Set the HTTP version for the proxy connection handshake.
     */
    public fun version(version: Version): HttpProxyConnectorLayer {
        this.customVersion = version
        return this
    }

    /**
     * Fluent alias for [version].
     */
    public fun withVersion(version: Version): HttpProxyConnectorLayer = version(version)

    /**
     * Mutate the HTTP version.
     */
    public fun setVersion(version: Version) {
        this.customVersion = version
    }

    /**
     * Add a custom header to the proxy connection handshake request.
     */
    public fun customHeader(name: String, value: String): HttpProxyConnectorLayer {
        val h = customHeaders ?: HeaderMap().also { customHeaders = it }
        h.insert(name, value)
        return this
    }

    /**
     * Fluent alias for [customHeader].
     */
    public fun withCustomHeader(name: String, value: String): HttpProxyConnectorLayer = customHeader(name, value)

    /**
     * Mutate a custom header.
     */
    public fun setCustomHeader(name: String, value: String) {
        customHeader(name, value)
    }

    override fun layer(inner: Any): HttpProxyConnector {
        val service = HttpProxyConnector.new(inner, required)
        customVersion?.let { service.setVersion(it) }
        customHeaders?.let { h ->
            for ((k, v) in h.toMap()) {
                service.setCustomHeader(k, v)
            }
        }
        return service
    }

    override fun intoLayer(inner: Any): HttpProxyConnector = layer(inner)

    public companion object {
        /**
         * Create an optional HTTP proxy connector layer.
         */
        public fun optional(): HttpProxyConnectorLayer =
            HttpProxyConnectorLayer(required = false)

        /**
         * Create a required HTTP proxy connector layer.
         */
        public fun required(): HttpProxyConnectorLayer =
            HttpProxyConnectorLayer(required = true)

        /**
         * Create a default HTTP proxy connector layer.
         */
        public fun default(): HttpProxyConnectorLayer = optional()
    }
}
