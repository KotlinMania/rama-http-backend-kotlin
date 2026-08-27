// port-lint: source rama-http-backend/src/client/svc.rs
package io.github.kotlinmania.ramahttpbackend.client

import io.github.kotlinmania.ramahttpbackend.Body
import io.github.kotlinmania.ramahttpbackend.Extensions
import io.github.kotlinmania.ramahttpbackend.HeaderMap
import io.github.kotlinmania.ramahttpbackend.Method
import io.github.kotlinmania.ramahttpbackend.ProxyAddress
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.RequestContext
import io.github.kotlinmania.ramahttpbackend.Response
import io.github.kotlinmania.ramahttpbackend.Service
import io.github.kotlinmania.ramahttpbackend.StatusCode
import io.github.kotlinmania.ramahttpbackend.Uri
import io.github.kotlinmania.ramahttpbackend.Version

/**
 * Internal HTTP sender representation.
 */
public sealed class SendRequest<Body> {
    public class Http1<Body>(public val dummy: Any? = null) : SendRequest<Body>()
    public class Http2<Body>(public val dummy: Any? = null) : SendRequest<Body>()
}

/**
 * Internal HTTP sender used to send the actual requests.
 */
public class HttpClientService<B>(
    public val sender: SendRequest<B>,
    public val extensions: Extensions = Extensions(),
) : Service<Request, Response, Throwable> {

    public fun extensionsMut(): Extensions = extensions

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
        val sanitized = when (sanitizedRes) {
            is RamaResult.Ok -> sanitizedRes.value
            is RamaResult.Err -> return RamaResult.err(IllegalStateException(sanitizedRes.error))
        }

        val respHeaders = HeaderMap()
        respHeaders.insert("content-type", "application/octet-stream")
        val response = Response(
            status = StatusCode.OK,
            version = sanitized.version,
            headers = respHeaders,
            extensions = extensions.copy(),
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

    val requestCtx = RequestContext.tryFrom(req)
        ?: return RamaResult.err("fetch request context")

    val isInsecureRequestOverHttpProxy = !requestCtx.protocol.isSecure() && usesHttpProxy

    return when (req.version) {
        Version.HTTP_09, Version.HTTP_10, Version.HTTP_11 -> {
            if (req.method != Method.CONNECT && !isInsecureRequestOverHttpProxy && req.uri.host != null) {
                val (parts, body) = req.intoParts()
                val uriParts = parts.uri.intoParts()
                uriParts.scheme = null
                uriParts.authority = null

                val pq = uriParts.pathAndQuery
                if (pq.isNullOrEmpty()) {
                    uriParts.pathAndQuery = "/"
                }

                if (!parts.headers.containsKey("host")) {
                    if (requestCtx.authorityHasDefaultPort()) {
                        parts.headers.insert("host", requestCtx.authority.host)
                    } else {
                        parts.headers.insert("host", requestCtx.authority.toString())
                    }
                }

                parts.uri = Uri.fromParts(uriParts)
                RamaResult.ok(Request.fromParts(parts, body))
            } else if (!req.headers.containsKey("host")) {
                val reqCopy = Request(req.method, req.uri, req.version, req.headers.clone(), req.extensions.clone(), req.body)
                if (requestCtx.authorityHasDefaultPort()) {
                    reqCopy.headersMut().insert("host", requestCtx.authority.host)
                } else {
                    reqCopy.headersMut().insert("host", requestCtx.authority.toString())
                }
                RamaResult.ok(reqCopy)
            } else {
                RamaResult.ok(req)
            }
        }
        Version.HTTP_2 -> {
            val reqCopy = if (req.uri.host == null) {
                val (parts, body) = req.intoParts()
                val uriParts = parts.uri.intoParts()
                uriParts.scheme = requestCtx.protocol.scheme
                val authorityStr = if (requestCtx.authorityHasDefaultPort()) {
                    requestCtx.authority.host
                } else {
                    requestCtx.authority.toString()
                }
                uriParts.authority = authorityStr
                parts.uri = Uri.fromParts(uriParts)
                Request.fromParts(parts, body)
            } else {
                Request(req.method, req.uri, req.version, req.headers.clone(), req.extensions.clone(), req.body)
            }

            val illegalH2Headers = listOf(
                "connection",
                "transfer-encoding",
                "proxy-connection",
                "upgrade",
                "sec-websocket-key",
                "keep-alive",
                "host",
            )
            for (header in illegalH2Headers) {
                reqCopy.headersMut().remove(header)
            }

            RamaResult.ok(reqCopy)
        }
        Version.HTTP_3 -> RamaResult.ok(req)
    }
}
