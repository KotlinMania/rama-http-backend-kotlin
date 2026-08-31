// port-lint: source rama-http-backend/src/client/proxy/layer/proxy_connector/proxy_error.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector

/**
 * Error that can be returned in case an HTTP proxy did not manage to establish a connection.
 */
public sealed class HttpProxyError {
    /**
     * Proxy Authentication Required (HTTP 407).
     */
    public object AuthRequired : HttpProxyError()

    /**
     * Proxy is Unavailable (HTTP 503).
     */
    public object Unavailable : HttpProxyError()

    /**
     * Transport error happened as part of HTTP proxy connection establishment.
     */
    public class Transport(
        public val error: HttpProxyCause,
    ) : HttpProxyError()

    /**
     * Something went wrong, but classification did not happen.
     */
    public class Other(
        public val header: String,
    ) : HttpProxyError()

    public val message: String
        get() = fmt()

    internal fun fmt(): String =
        when (this) {
            is AuthRequired -> "http proxy error: proxy auth required (http 407)"
            is Unavailable -> "http proxy error: proxy unavailable (http 503)"
            is Transport -> "http proxy error: transport error: I/O [$error]"
            is Other -> "http proxy error: first line of header = [$header]"
        }

    override fun toString(): String = fmt()

    /**
     * The underlying error that caused this proxy failure, when one is available.
     */
    public fun source(): HttpProxyCause? =
        when (this) {
            is Transport -> {
                error.source ?: error
            }
            else -> null
        }

    public companion object {
        /**
         * Create a proxy transport error from an I/O failure description.
         */
        public fun from(value: HttpProxyCause): HttpProxyError = Transport(value)

        /**
         * Create a proxy transport error from a Throwable.
         */
        public fun from(value: Throwable): HttpProxyError = Transport(HttpProxyCause.fromThrowable(value))

        /**
         * Create a proxy transport error from an I/O failure description.
         */
        public fun fromTransport(value: HttpProxyCause): HttpProxyError = Transport(value)
    }
}

/**
 * Underlying cause of an [HttpProxyError.Transport].
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
 * Best-effort conversion of a [Throwable] into a [HttpProxyCause] chain.
 */
internal fun HttpProxyCause.Companion.fromThrowable(value: Throwable): HttpProxyCause =
    HttpProxyCause(
        text = value.message ?: value.toString(),
        source = value.cause?.let { fromThrowable(it) },
    )
