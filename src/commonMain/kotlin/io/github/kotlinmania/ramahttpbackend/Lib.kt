// port-lint: source lib.rs
package io.github.kotlinmania.ramahttpbackend

import io.github.kotlinmania.ramahttp.types.HeaderName
import io.github.kotlinmania.ramahttp.types.Request
import io.github.kotlinmania.ramanet.http.RequestContext
import io.github.kotlinmania.ramanet.user.ProxyCredential

/**
 * Default Rama HTTP backend for Kotlin Multiplatform.
 */
public object RamaHttpBackend {
    public const val VERSION: String = "0.2.0"
}

internal fun tryExtractRequestContext(req: Request): RequestContext? {
    req.extensions.get<RequestContext>()?.let { return it }
    return RequestContext
        .fromParts(
            uriScheme = req.uri.scheme?.asStr,
            uriHost = req.uri.host,
            uriPort =
                req.uri.authority
                    ?.portU16
                    ?.toUShort(),
            hostHeader = req.headers.get(HeaderName.HOST)?.toStr(),
            httpVersion = req.version.asStr(),
            extensions = req.extensions,
        ).getOrNull()
}

internal fun RequestContext.hasDefaultPort(): Boolean =
    authority.port == null || authorityHasDefaultPort()

internal fun ProxyCredential.headerValue(): String =
    when (this) {
        is ProxyCredential.BasicAuth -> {
            val raw = if (basic.password != null) "${basic.username}:${basic.password}" else basic.username
            "Basic " + raw.encodeToByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        }

        is ProxyCredential.BearerAuth -> {
            "Bearer ${bearer.unmasked()}"
        }
    }
