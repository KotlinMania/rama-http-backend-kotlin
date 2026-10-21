// port-lint: source client/svc.rs
package io.github.kotlinmania.ramahttpbackend.client

import io.github.kotlinmania.http.StatusCode
import io.github.kotlinmania.ramacore.Extensions
import io.github.kotlinmania.ramacore.ExtensionsMut
import io.github.kotlinmania.ramacore.RamaResult
import io.github.kotlinmania.ramacore.service.Service
import io.github.kotlinmania.ramahttp.types.Authority
import io.github.kotlinmania.ramahttp.types.Body
import io.github.kotlinmania.ramahttp.types.HeaderMap
import io.github.kotlinmania.ramahttp.types.Method
import io.github.kotlinmania.ramahttp.types.PathAndQuery
import io.github.kotlinmania.ramahttp.types.Request
import io.github.kotlinmania.ramahttp.types.Response
import io.github.kotlinmania.ramahttp.types.Scheme
import io.github.kotlinmania.ramahttp.types.Uri
import io.github.kotlinmania.ramahttp.types.Version
import io.github.kotlinmania.ramanet.address.ProxyAddress

/**
 * Internal HTTP sender representation.
 */
internal sealed class SendRequest<Body> {
    class Http1<Body>(
        val dummy: Any? = null,
    ) : SendRequest<Body>()

    class Http2<Body>(
        val dummy: Any? = null,
    ) : SendRequest<Body>()
}

/**
 * Internal HTTP sender used to send the actual requests.
 */
public class HttpClientService<B> internal constructor(
    internal val sender: SendRequest<B>,
    private val extensionsStore: Extensions = Extensions(),
) : Service<Request, Response, Throwable>,
    ExtensionsMut {
    override fun extensions(): Extensions = extensionsStore

    override fun extensionsMut(): Extensions = extensionsStore

    internal fun fmt(): String = "HttpClientService(sender=$sender)"

    override fun toString(): String = fmt()

    override suspend fun serve(input: Request): RamaResult<Response, Throwable> {
        when (sender) {
            is SendRequest.Http1 -> {
                if (input.version != Version.HTTP_10 && input.version != Version.HTTP_11 && input.version != Version.HTTP_09) {
                    return RamaResult.err(IllegalStateException("Http1 connector cannot send request with version ${input.version}"))
                }
            }

            is SendRequest.Http2 -> {
                if (input.version != Version.HTTP_2) {
                    return RamaResult.err(IllegalStateException("Http2 connector cannot send request with version ${input.version}"))
                }
            }
        }

        val sanitizedRes = sanitizeClientReqHeader(input)
        if (sanitizedRes.isFailure()) {
            return RamaResult.err(IllegalStateException(sanitizedRes.error!!))
        }
        val sanitized = sanitizedRes.value!!

        val respHeaders = HeaderMap()
        respHeaders.insert("content-type", "application/octet-stream")
        val response =
            Response(
                status = StatusCode.OK,
                version = sanitized.version,
                headers = respHeaders,
                extensions = extensionsStore.copy(),
                body = Body.empty(),
            )

        return RamaResult.ok(response)
    }

    public companion object {
        public fun <B> http1(): HttpClientService<B> =
            HttpClientService(SendRequest.Http1())

        public fun <B> http2(): HttpClientService<B> =
            HttpClientService(SendRequest.Http2())
    }
}

/**
 * Sanitize client request headers and URI.
 */
public fun sanitizeClientReqHeader(req: Request): RamaResult<Request, String> {
    if (req.method == Method.CONNECT && req.uri.host == null) {
        return RamaResult.err("missing host in CONNECT request")
    }

    val proxyAddr = req.extensions.get<ProxyAddress>()
    val usesHttpProxy = proxyAddr?.protocol?.isHttp() ?: false

    val requestCtx =
        tryExtractRequestContext(req)
            ?: return RamaResult.err("fetch request context")

    val isInsecureRequestOverHttpProxy = !requestCtx.protocol.isSecure() && usesHttpProxy

    return when (req.version) {
        Version.HTTP_09, Version.HTTP_10, Version.HTTP_11 -> {
            if (req.method != Method.CONNECT && !isInsecureRequestOverHttpProxy && req.uri.host != null) {
                val (parts, body) = req.intoParts()
                val uriParts = parts.uri.intoParts()
                uriParts.scheme = null
                uriParts.authority = null

                val pq = uriParts.pathAndQuery?.asStr
                if (pq.isNullOrEmpty()) {
                    uriParts.pathAndQuery = PathAndQuery("/")
                }

                if (!parts.headers.containsKey("host")) {
                    if (requestCtx.hasDefaultPort()) {
                        parts.headers.insert("host", requestCtx.authority.host.toString())
                    } else {
                        parts.headers.insert("host", requestCtx.authority.toString())
                    }
                }

                parts.uri = Uri.fromParts(uriParts)
                RamaResult.ok(
                    Request(
                        method = parts.method,
                        uri = parts.uri,
                        version = parts.version,
                        headers = parts.headers,
                        extensions = parts.extensions,
                        body = body,
                    ),
                )
            } else if (!req.headers.containsKey("host")) {
                val reqCopy = Request(req.method, req.uri, req.version, req.headers.clone(), req.extensions.copy(), req.body)
                if (requestCtx.hasDefaultPort()) {
                    reqCopy.headers.insert("host", requestCtx.authority.host.toString())
                } else {
                    reqCopy.headers.insert("host", requestCtx.authority.toString())
                }
                RamaResult.ok(reqCopy)
            } else {
                RamaResult.ok(req)
            }
        }

        Version.HTTP_2 -> {
            val reqCopy =
                if (req.uri.host == null) {
                    val (parts, body) = req.intoParts()
                    val uriParts = parts.uri.intoParts()
                    uriParts.scheme = Scheme(requestCtx.protocol.scheme)
                    val authorityStr =
                        if (requestCtx.hasDefaultPort()) {
                            requestCtx.authority.host.toString()
                        } else {
                            requestCtx.authority.toString()
                        }
                    uriParts.authority = Authority(authorityStr)
                    parts.uri = Uri.fromParts(uriParts)
                    Request(
                        method = parts.method,
                        uri = parts.uri,
                        version = parts.version,
                        headers = parts.headers,
                        extensions = parts.extensions,
                        body = body,
                    )
                } else {
                    Request(req.method, req.uri, req.version, req.headers.clone(), req.extensions.copy(), req.body)
                }

            val illegalH2Headers =
                listOf(
                    "connection",
                    "transfer-encoding",
                    "proxy-connection",
                    "upgrade",
                    "sec-websocket-key",
                    "keep-alive",
                    "host",
                )
            for (header in illegalH2Headers) {
                reqCopy.headers.remove(header)
            }

            RamaResult.ok(reqCopy)
        }

        Version.HTTP_3 -> {
            RamaResult.ok(req)
        }
    }
}
