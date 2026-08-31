// port-lint: source rama-http-backend/src/server/service.rs
package io.github.kotlinmania.ramahttpbackend.server

import io.github.kotlinmania.ramahttpbackend.Executor
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Service
import io.github.kotlinmania.ramahttpbackend.ShutdownGuard
import io.github.kotlinmania.ramahttpbackend.Stream

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
     * Turn this [HttpServer] into a [Service] that can be used to serve IO byte streams as HTTP.
     */
    public fun service(service: Any): Service<Stream, Unit, Throwable> =
        HttpService(builder, service)

    /**
     * Serve a single IO byte stream as HTTP.
     */
    public suspend fun serve(
        stream: Stream,
        service: Any,
    ): HttpServeResult = builder.httpCoreServeConnection(stream, service)

    /**
     * Listen for connections on the given network interface.
     */
    public suspend fun listen(
        interfaceAddress: Any,
        service: Any,
    ): HttpServeResult = RamaResult.ok(Unit)

    /**
     * Listen for connections on the given Unix socket path.
     */
    public suspend fun listenUnix(
        path: Any,
        service: Any,
    ): HttpServeResult = RamaResult.ok(Unit)

    public companion object {
        /**
         * Create a new HTTP/1.1 server builder.
         */
        public fun http1(): HttpServer<Http1Builder> =
            HttpServer(Http1Builder.new(), null)

        /**
         * Create a new HTTP/2 server builder.
         */
        public fun h2(exec: Executor = Executor.default): HttpServer<Http2Builder> =
            HttpServer(Http2Builder.new(exec), exec.guard())

        /**
         * Create a new dual HTTP/1.1 + HTTP/2 auto server builder.
         */
        public fun auto(exec: Executor = Executor.default): HttpServer<AutoBuilder> =
            HttpServer(AutoBuilder.new(exec), exec.guard())

        /**
         * Create a default HTTP server builder.
         */
        public fun default(): HttpServer<AutoBuilder> = auto(Executor.default)
    }
}

/**
 * A [Service] that can be used to serve IO byte streams as HTTP.
 */
internal class HttpService<B : HttpCoreConnServer>(
    public val builder: B,
    public val service: Any,
) : Service<Stream, Unit, Throwable> {
    override suspend fun serve(input: Stream): RamaResult<Unit, Throwable> =
        builder.httpCoreServeConnection(input, service)

    companion object {
        fun <B : HttpCoreConnServer> new(
            builder: B,
            service: Any,
        ): HttpService<B> = HttpService(builder, service)
    }
}
