// port-lint: source client/proxy/layer/proxy_connector/proxy_error.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector

/**
 * error that can be returned in case a http proxy
 * did not manage to establish a connection
 */
public sealed class HttpProxyError : Throwable() {
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
    public class Transport(public val error: Throwable) : HttpProxyError()

    /**
     * Something went wrong, but classification did not happen.
     *
     * (First header line of http response is included in error)
     */
    public class Other(public val header: String) : HttpProxyError()

    override val message: String
        get() = when (this) {
            is AuthRequired -> "http proxy error: proxy auth required (http 407)"
            is Unavailable -> "http proxy error: proxy unavailable (http 503)"
            is Transport -> "http proxy error: transport error: I/O [$error]"
            is Other -> "http proxy error: first line of header = [$header]"
        }

    override fun toString(): String = message

    override val cause: Throwable?
        get() = source()

    /**
     * The underlying error that caused this proxy failure, when one is available.
     */
    public fun source(): Throwable? = when (this) {
        is Transport -> {
            // filter out generic io errors,
            // but do allow custom errors (e.g. because IP is blocked)
            error.cause ?: error
        }
        else -> null
    }

    public companion object {
        /**
         * Create a proxy transport error from an I/O failure.
         */
        public fun from(value: Throwable): HttpProxyError = Transport(value)
    }
}
