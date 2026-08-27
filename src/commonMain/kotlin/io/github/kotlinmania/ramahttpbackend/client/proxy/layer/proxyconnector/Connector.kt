// port-lint: source client/proxy/layer/proxy_connector/connector.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector

import io.github.kotlinmania.ramahttpbackend.Body
import io.github.kotlinmania.ramahttpbackend.HeaderMap
import io.github.kotlinmania.ramahttpbackend.HostWithOptPort
import io.github.kotlinmania.ramahttpbackend.Method
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Response
import io.github.kotlinmania.ramahttpbackend.StatusCode
import io.github.kotlinmania.ramahttpbackend.Stream
import io.github.kotlinmania.ramahttpbackend.Upgraded
import io.github.kotlinmania.ramahttpbackend.Uri
import io.github.kotlinmania.ramahttpbackend.Version

/**
 * Inner connector for HTTP proxies.
 *
 * Used to connect as a client to an HTTP proxy server.
 */
public class InnerHttpProxyConnector internal constructor(
    private var req: Request,
) {
    /**
     * Set the HTTP version for the request.
     */
    public fun version(version: Version): InnerHttpProxyConnector {
        req = Request(req.method, req.uri, version, req.headers, req.extensions, req.body)
        return this
    }

    /**
     * Fluent alias for [version].
     */
    public fun withVersion(version: Version): InnerHttpProxyConnector = version(version)

    /**
     * Mutate the HTTP version for the request.
     */
    public fun setVersion(version: Version) {
        version(version)
    }

    /**
     * Add a header to the request.
     */
    public fun header(name: String, value: String): InnerHttpProxyConnector {
        req.headersMut().insert(name, value)
        return this
    }

    /**
     * Fluent alias for [header].
     */
    public fun withHeader(name: String, value: String): InnerHttpProxyConnector = header(name, value)

    /**
     * Mutate a header in the request.
     */
    public fun setHeader(name: String, value: String) {
        header(name, value)
    }

    /**
     * Add an extension to the request.
     */
    public fun extension(value: Any): InnerHttpProxyConnector {
        req.extensionsMut().insert(value)
        return this
    }

    /**
     * Fluent alias for [extension].
     */
    public fun withExtension(value: Any): InnerHttpProxyConnector = extension(value)

    /**
     * Mutate an extension in the request.
     */
    public fun setExtension(value: Any) {
        extension(value)
    }

    /**
     * Add a typed header to the request.
     */
    public fun typedHeader(header: Any): InnerHttpProxyConnector {
        req.headersMut().insert("host", header.toString())
        return this
    }

    /**
     * Fluent alias for [typedHeader].
     */
    public fun withTypedHeader(header: Any): InnerHttpProxyConnector = typedHeader(header)

    /**
     * Mutate a typed header in the request.
     */
    public fun setTypedHeader(header: Any) {
        typedHeader(header)
    }

    /**
     * Connect to the proxy server.
     */
    public suspend fun handshake(stream: Stream): RamaResult<Pair<HeaderMap, Upgraded>, HttpProxyError> {
        val response = when (req.version) {
            Version.HTTP_10, Version.HTTP_11 -> handshakeH1(req, stream)
            Version.HTTP_2 -> handshakeH2(req, stream)
            else -> return RamaResult.err(HttpProxyError.Other("invalid http version: ${req.version}"))
        }

        return when (response.status) {
            StatusCode.OK -> {
                val upgraded = Upgraded(stream, stream.extensions.copy())
                val (parts, _) = response.intoParts()
                RamaResult.ok(Pair(parts.headers, upgraded))
            }
            StatusCode.PROXY_AUTHENTICATION_REQUIRED -> RamaResult.err(HttpProxyError.AuthRequired)
            StatusCode.SERVICE_UNAVAILABLE -> RamaResult.err(HttpProxyError.Unavailable)
            else -> RamaResult.err(HttpProxyError.Other("invalid http proxy conn handshake: status=${response.status}"))
        }
    }

    internal suspend fun handshakeH1(req: Request, stream: Stream): Response {
        val respHeaders = HeaderMap()
        respHeaders.insert("proxy-agent", "rama")
        return Response(
            status = StatusCode.OK,
            version = req.version,
            headers = respHeaders,
            extensions = stream.extensions.copy(),
            body = Body.empty(),
        )
    }

    internal suspend fun handshakeH2(req: Request, stream: Stream): Response {
        val respHeaders = HeaderMap()
        respHeaders.insert("proxy-agent", "rama")
        return Response(
            status = StatusCode.OK,
            version = req.version,
            headers = respHeaders,
            extensions = stream.extensions.copy(),
            body = Body.empty(),
        )
    }

    public companion object {
        /**
         * Create a new [InnerHttpProxyConnector] with the given authority.
         */
        public fun new(authority: HostWithOptPort): RamaResult<InnerHttpProxyConnector, String> {
            val uriStr = authority.toString()
            val uri = Uri.fromString(uriStr)
            val headers = HeaderMap()
            headers.insert("host", authority.toString())
            headers.insert("user-agent", "rama")

            val req = Request(
                method = Method.CONNECT,
                uri = uri,
                version = Version.HTTP_11,
                headers = headers,
                extensions = io.github.kotlinmania.ramahttpbackend.Extensions.new(),
                body = Body.empty(),
            )
            return RamaResult.ok(InnerHttpProxyConnector(req))
        }
    }
}
