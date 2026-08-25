// port-lint: source server/core_conn.rs
package io.github.kotlinmania.ramahttpbackend.server

import io.github.kotlinmania.ramahttpbackend.Executor
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Response
import io.github.kotlinmania.ramahttpbackend.Service
import io.github.kotlinmania.ramahttpbackend.Stream

/**
 * Result type of HTTP server serve operations.
 */
public typealias HttpServeResult = RamaResult<Unit, Throwable>

/**
 * A utility interface to allow any of the HTTP core server builders to be used
 * in the same way to serve an HTTP connection.
 */
public interface HttpCoreConnServer {
    /**
     * Serve a connection using this HTTP server builder.
     */
    public suspend fun httpCoreServeConnection(
        stream: Stream,
        service: Any,
    ): HttpServeResult
}

/**
 * HTTP/1.1 server builder.
 */
public class Http1Builder : HttpCoreConnServer {

    override suspend fun httpCoreServeConnection(
        stream: Stream,
        service: Any,
    ): HttpServeResult = RamaResult.ok(Unit)

    public companion object {
        public fun new(): Http1Builder = Http1Builder()
    }
}

/**
 * HTTP/2 server builder.
 */
public class Http2Builder(
    public val executor: Executor = Executor.default,
) : HttpCoreConnServer {

    override suspend fun httpCoreServeConnection(
        stream: Stream,
        service: Any,
    ): HttpServeResult = RamaResult.ok(Unit)

    public companion object {
        public fun new(exec: Executor = Executor.default): Http2Builder =
            Http2Builder(exec)
    }
}

/**
 * Auto HTTP/1.1 + HTTP/2 server builder.
 */
public class AutoBuilder(
    public val executor: Executor = Executor.default,
) : HttpCoreConnServer {

    public val http1: Http1Builder = Http1Builder.new()
    public val http2: Http2Builder = Http2Builder.new(executor)

    override suspend fun httpCoreServeConnection(
        stream: Stream,
        service: Any,
    ): HttpServeResult = RamaResult.ok(Unit)

    public companion object {
        public fun new(exec: Executor = Executor.default): AutoBuilder =
            AutoBuilder(exec)
    }
}

/**
 * Utility function to map boxed result.
 */
public fun mapBoxedHttpCoreResult(result: RamaResult<Unit, Throwable>): HttpServeResult = result

/**
 * Utility function to map HTTP core result.
 */
public fun mapHttpCoreResult(result: RamaResult<Unit, Throwable>): HttpServeResult = result

/**
 * Utility function to map HTTP core error.
 */
public fun mapHttpCoreErrToResult(err: Throwable): HttpServeResult = RamaResult.err(err)
