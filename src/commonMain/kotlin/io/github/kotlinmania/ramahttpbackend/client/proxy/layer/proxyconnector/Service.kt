// port-lint: source client/proxy/layer/proxy_connector/service.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector

import io.github.kotlinmania.ramacore.Extensions
import io.github.kotlinmania.ramacore.ExtensionsMut
import io.github.kotlinmania.ramacore.RamaResult
import io.github.kotlinmania.ramacore.service.Service
import io.github.kotlinmania.ramahttp.io.Upgraded
import io.github.kotlinmania.ramahttp.types.HeaderMap
import io.github.kotlinmania.ramahttp.types.Request
import io.github.kotlinmania.ramahttp.types.Version
import io.github.kotlinmania.ramahttpbackend.headerValue
import io.github.kotlinmania.ramahttpbackend.tryExtractRequestContext
import io.github.kotlinmania.ramanet.address.HostWithOptPort
import io.github.kotlinmania.ramanet.address.ProxyAddress
import io.github.kotlinmania.ramanet.client.EstablishedClientConnection

/**
 * A connector which can be used to establish a connection over an HTTP Proxy.
 */
public class HttpProxyConnector(
    public val inner: Any = Any(),
    public val required: Boolean = false,
    private var customVersion: Version? = Version.HTTP_11,
    private var customHeaders: HeaderMap? = null,
) : Service<Request, EstablishedClientConnection<MaybeHttpProxiedConnection, Request>, Throwable> {
    public val version: Version? get() = customVersion
    public val headers: HeaderMap? get() = customHeaders

    /**
     * Set the HTTP version to use for the CONNECT request.
     */
    public fun version(version: Version): HttpProxyConnector {
        this.customVersion = version
        return this
    }

    /**
     * Fluent alias for [version].
     */
    public fun withVersion(version: Version): HttpProxyConnector = version(version)

    /**
     * Mutate the HTTP version.
     */
    public fun setVersion(version: Version) {
        this.customVersion = version
    }

    /**
     * Append a custom header to use for the CONNECT request.
     */
    public fun customHeader(name: String, value: String): HttpProxyConnector {
        val h = customHeaders ?: HeaderMap().also { customHeaders = it }
        h.insert(name, value)
        return this
    }

    /**
     * Fluent alias for [customHeader].
     */
    public fun withCustomHeader(name: String, value: String): HttpProxyConnector = customHeader(name, value)

    /**
     * Mutate a custom header.
     */
    public fun setCustomHeader(name: String, value: String) {
        customHeader(name, value)
    }

    /**
     * Mutable reference to inner service.
     */
    public fun innerMut(): Any = inner

    override suspend fun serve(
        input: Request,
    ): RamaResult<EstablishedClientConnection<MaybeHttpProxiedConnection, Request>, Throwable> {
        val proxyInfo = input.extensions.get<ProxyAddress>()
        if (proxyInfo?.protocol != null && !proxyInfo.protocol!!.isHttp()) {
            return RamaResult.err(IllegalArgumentException("http proxy connector can only serve http protocol"))
        }

        val requestCtx = tryExtractRequestContext(input)

        if (proxyInfo == null) {
            if (required) {
                return RamaResult.err(IllegalStateException("http proxy required but none is defined"))
            }
            val mockStream: ExtensionsMut =
                object : ExtensionsMut {
                    private val ext: Extensions = input.extensions.copy()

                    override fun extensions(): Extensions = ext

                    override fun extensionsMut(): Extensions = ext
                }
            val conn: MaybeHttpProxiedConnection = MaybeHttpProxiedConnection.direct(mockStream)
            return RamaResult.ok(EstablishedClientConnection(conn, input))
        }

        val authority = requestCtx?.authority ?: HostWithOptPort(proxyInfo.address.host, proxyInfo.address.port)

        if (requestCtx != null && !requestCtx.protocol.isSecure()) {
            val mockStream: ExtensionsMut =
                object : ExtensionsMut {
                    private val ext: Extensions = input.extensions.copy()

                    override fun extensions(): Extensions = ext

                    override fun extensionsMut(): Extensions = ext
                }
            val conn: MaybeHttpProxiedConnection = MaybeHttpProxiedConnection.proxied(mockStream)
            return RamaResult.ok(EstablishedClientConnection(conn, input))
        }

        val connectorResult = InnerHttpProxyConnector.new(authority)
        val connector =
            when (connectorResult) {
                is RamaResult.Ok -> connectorResult.value
                is RamaResult.Err -> return RamaResult.err(IllegalStateException(connectorResult.error))
            }

        customVersion?.let { connector.setVersion(it) }

        proxyInfo.credential?.let { cred ->
            connector.setHeader("proxy-authorization", cred.headerValue())
        }

        customHeaders?.let { h ->
            for ((k, v) in h.entries()) {
                val key = k.asStr()
                if (!key.equals("proxy-authorization", ignoreCase = true) && !key.equals("host", ignoreCase = true)) {
                    connector.setHeader(key, v.toStr())
                }
            }
        }

        val mockStream: ExtensionsMut =
            object : ExtensionsMut {
                private val ext: Extensions = input.extensions.copy()

                override fun extensions(): Extensions = ext

                override fun extensionsMut(): Extensions = ext
            }

        return when (val handshakeRes = connector.handshake(mockStream)) {
            is RamaResult.Ok -> {
                val (respHeaders, upgraded) = handshakeRes.value
                upgraded.extensionsMut().insert(HttpProxyConnectResponseHeaders.new(respHeaders))
                val conn: MaybeHttpProxiedConnection = MaybeHttpProxiedConnection.upgradedProxy(upgraded)
                RamaResult.ok(EstablishedClientConnection(conn, input))
            }

            is RamaResult.Err -> {
                RamaResult.err(IllegalStateException(handshakeRes.error.message))
            }
        }
    }

    public companion object {
        /**
         * Creates a new [HttpProxyConnector].
         */
        public fun new(inner: Any = Any(), required: Boolean = false): HttpProxyConnector =
            HttpProxyConnector(inner, required)

        /**
         * Create an optional [HttpProxyConnector].
         */
        public fun optional(inner: Any = Any()): HttpProxyConnector =
            new(inner, false)

        /**
         * Create a required [HttpProxyConnector].
         */
        public fun required(inner: Any = Any()): HttpProxyConnector =
            new(inner, true)
    }
}

/**
 * Extension added to the context by [HttpProxyConnector] to record the headers from a successful CONNECT response.
 */
public class HttpProxyConnectResponseHeaders(
    public val headers: HeaderMap,
) {
    public fun asRef(): HeaderMap = headers

    public fun deref(): HeaderMap = headers

    override fun toString(): String = headers.toString()

    public companion object {
        public fun new(headers: HeaderMap): HttpProxyConnectResponseHeaders =
            HttpProxyConnectResponseHeaders(headers)
    }
}

/**
 * A connection which will be proxied if a [ProxyAddress] was configured.
 */
public class MaybeHttpProxiedConnection private constructor(
    private val directConn: ExtensionsMut? = null,
    private val proxiedConn: ExtensionsMut? = null,
    private val upgradedConn: Upgraded? = null,
) : ExtensionsMut {
    public val isDirect: Boolean get() = directConn != null
    public val isProxied: Boolean get() = proxiedConn != null
    public val isUpgradedProxy: Boolean get() = upgradedConn != null

    private val defaultExt: Extensions = Extensions.new()

    override fun extensions(): Extensions =
        directConn?.extensions() ?: proxiedConn?.extensions() ?: upgradedConn?.extensions() ?: defaultExt

    override fun extensionsMut(): Extensions = extensions()

    override fun toString(): String =
        when {
            directConn != null -> "MaybeHttpProxiedConnection.Direct($directConn)"
            proxiedConn != null -> "MaybeHttpProxiedConnection.Proxied($proxiedConn)"
            upgradedConn != null -> "MaybeHttpProxiedConnection.UpgradedProxy($upgradedConn)"
            else -> "MaybeHttpProxiedConnection"
        }

    public companion object {
        public fun direct(conn: ExtensionsMut): MaybeHttpProxiedConnection =
            MaybeHttpProxiedConnection(directConn = conn)

        public fun proxied(conn: ExtensionsMut): MaybeHttpProxiedConnection =
            MaybeHttpProxiedConnection(proxiedConn = conn)

        public fun upgradedProxy(conn: Upgraded): MaybeHttpProxiedConnection =
            MaybeHttpProxiedConnection(upgradedConn = conn)
    }
}
