// port-lint: source client/proxy/layer/proxy_connector/proxy_error.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector

/**
 * error that can be returned in case a http proxy
 * did not manage to establish a connection
 */
public sealed class HttpProxyError {
    /**
     * Proxy Authentication Required
     *
     * (Proxy returned HTTP 407)
     */
    public object AuthRequired : HttpProxyError()

    /**
     * Proxy is Unavailable
     *
     * (Proxy returned HTTP 503)
     */
    public object Unavailable : HttpProxyError()

    /**
     * I/O error happened as part of HTTP Proxy Connection Establishment
     *
     * (e.g. some kind of TCP error)
     */
    public class Transport(public val error: HttpProxyCause) : HttpProxyError()

    /**
     * Something went wrong, but classification did not happen.
     *
     * (First header line of http response is included in error)
     */
    public class Other(public val header: String) : HttpProxyError()

    public val message: String
        get() = fmt()

    internal fun fmt(): String = when (this) {
        is AuthRequired -> "http proxy error: proxy auth required (http 407)"
        is Unavailable -> "http proxy error: proxy unavailable (http 503)"
        is Transport -> "http proxy error: transport error: I/O [$error]"
        is Other -> "http proxy error: first line of header = [$header]"
    }

    override fun toString(): String = fmt()

    /**
     * The underlying error that caused this proxy failure, when one is available.
     */
    public fun source(): HttpProxyCause? = when (this) {
        is Transport -> {
            // filter out generic io errors,
            // but do allow custom errors (e.g. because IP is blocked)
            error.source ?: error
        }
        else -> null
    }

    public companion object {
        /**
         * Create a proxy transport error from an I/O failure description.
         */
        public fun fromTransport(value: HttpProxyCause): HttpProxyError = Transport(value)
    }
}

/**
 * Non-throwable carrier for the underlying cause of an
 * [HttpProxyError.Transport]. The Rust upstream uses
 * `BoxError = Box<dyn std::error::Error + Send + Sync>`; in Kotlin we cannot
 * use [Throwable] directly because the Swift Export bridge expands
 * `Throwable.suppressed: Array<Throwable>` into `Array<Any?>` casts that
 * fail under `allWarningsAsErrors`. `HttpProxyCause` mirrors the
 * `description` + nested-`source` chain a `dyn std::error::Error` exposes
 * without dragging `kotlin.Throwable` (and therefore `kotlin.Array`) into
 * the public API surface.
 */
public class HttpProxyCause(
    public val text: String,
    public val source: HttpProxyCause? = null,
) {
    override fun toString(): String = text

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpProxyCause) return false
        return text == other.text && source == other.source
    }

    override fun hashCode(): Int = 31 * text.hashCode() + (source?.hashCode() ?: 0)

    public companion object
}

/**
 * Best-effort conversion of a [Throwable] thrown by a transport
 * implementation into a [HttpProxyCause] chain. Internal so the Throwable
 * parameter type is never reached by the Swift Export bridge; downstream
 * Kotlin callers that need to wrap a real `Throwable` go through this
 * helper from the same module.
 */
internal fun HttpProxyCause.Companion.fromThrowable(value: Throwable): HttpProxyCause = HttpProxyCause(
    text = value.message ?: value.toString(),
    source = value.cause?.let { fromThrowable(it) },
)
