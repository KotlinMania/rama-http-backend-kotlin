// port-lint: source server/service.rs
package io.github.kotlinmania.ramahttpbackend.server

import io.github.kotlinmania.ramacore.ExtensionsMut
import io.github.kotlinmania.ramacore.RamaResult
import io.github.kotlinmania.ramacore.graceful.ShutdownGuard
import io.github.kotlinmania.ramacore.rt.Executor
import io.github.kotlinmania.ramacore.service.Service
import io.github.kotlinmania.ramanet.socket.Interface
import io.github.kotlinmania.ramatcp.server.TcpListener
import io.github.kotlinmania.ramaunix.unix.server.UnixListener

/**
 * A builder for configuring and listening over HTTP using a [Service].
 *
 * Supported Protocols: HTTP/1, H2, Auto (HTTP/1 + H2).
 */
public class HttpServer<B : HttpCoreConnServer>(
    public val builder: B,
    private var shutdownGuard: ShutdownGuard? = null,
) {
    public val guard: ShutdownGuard? get() = shutdownGuard

    /**
     * Set the shutdown guard for graceful connection draining.
     */
    public fun guard(guard: ShutdownGuard?): HttpServer<B> {
        this.shutdownGuard = guard
        return this
    }

    /**
     * Fluent alias for [guard].
     */
    public fun withGuard(guard: ShutdownGuard?): HttpServer<B> = guard(guard)

    /**
     * Mutate shutdown guard.
     */
    public fun setGuard(guard: ShutdownGuard?) {
        this.shutdownGuard = guard
    }

    /**
     * HTTP/1 configuration.
     */
    public fun http1Mut(): Http1Builder =
        when (val b = builder) {
            is Http1Builder -> b
            is AutoBuilder -> b.http1()
            else -> Http1Builder.new()
        }

    /**
     * HTTP/2 configuration.
     */
    public fun h2Mut(): Http2Builder =
        when (val b = builder) {
            is Http2Builder -> b
            is AutoBuilder -> b.http2()
            else -> Http2Builder.new()
        }

    /**
     * Turn this [HttpServer] into a [Service] that can be used to serve IO byte streams as HTTP.
     */
    public fun service(service: Any): Service<ExtensionsMut, Unit, Throwable> =
        HttpService(builder, service)

    /**
     * Serve a single IO byte stream as HTTP.
     */
    public suspend fun serve(
        stream: ExtensionsMut,
        service: Any,
    ): HttpServeResult = builder.httpCoreServeConnection(stream, service)

    /**
     * Listen for connections on the given network interface.
     */
    public suspend fun listen(
        interfaceAddress: Any,
        service: Any,
    ): HttpServeResult =
        try {
            val tcp =
                when (interfaceAddress) {
                    is Interface -> TcpListener.bind(interfaceAddress)
                    is String -> TcpListener.bind(interfaceAddress)
                    else -> TcpListener.bindSocket(interfaceAddress)
                }
            val httpService = HttpService.new(builder, service)
            val g = shutdownGuard
            if (g != null) {
                tcp.serveGraceful(g, httpService)
            } else {
                tcp.serve(httpService)
            }
            RamaResult.ok(Unit)
        } catch (e: Throwable) {
            RamaResult.err(e)
        }

    /**
     * Listen for connections on the given Unix socket path.
     */
    public suspend fun listenUnix(
        path: Any,
        service: Any,
    ): HttpServeResult =
        try {
            val socket = UnixListener.bindPath(path.toString())
            val httpService = HttpService.new(builder, service)
            val g = shutdownGuard
            if (g != null) {
                socket.serveGraceful(g, httpService)
            } else {
                socket.serve(httpService)
            }
            RamaResult.ok(Unit)
        } catch (e: Throwable) {
            RamaResult.err(e)
        }

    public companion object {
        /**
         * Create a new HTTP/1.1 server builder.
         */
        public fun http1(): HttpServer<Http1Builder> =
            HttpServer(Http1Builder.new(), null)

        /**
         * Create a new HTTP/2 server builder.
         */
        public fun h2(exec: Executor = Executor.new()): HttpServer<Http2Builder> =
            HttpServer(Http2Builder.new(exec), exec.guard())

        /**
         * Create a new dual HTTP/1.1 + HTTP/2 auto server builder.
         */
        public fun auto(exec: Executor = Executor.new()): HttpServer<AutoBuilder> =
            HttpServer(AutoBuilder.new(exec), exec.guard())

        /**
         * Create a default HTTP server builder.
         */
        public fun default(): HttpServer<AutoBuilder> = auto(Executor.new())
    }
}

/**
 * A [Service] that can be used to serve IO byte streams as HTTP.
 */
internal class HttpService<B : HttpCoreConnServer>(
    public val builder: B,
    public val service: Any,
) : Service<ExtensionsMut, Unit, Throwable> {
    override suspend fun serve(input: ExtensionsMut): RamaResult<Unit, Throwable> =
        builder.httpCoreServeConnection(input, service)

    internal fun fmt(): String = "HttpService(builder=$builder, service=$service)"

    override fun toString(): String = fmt()

    public fun clone(): HttpService<B> = HttpService(builder, service)

    companion object {
        fun <B : HttpCoreConnServer> new(
            builder: B,
            service: Any,
        ): HttpService<B> = HttpService(builder, service)
    }
}
