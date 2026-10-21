// port-lint: source server/core_conn.rs
package io.github.kotlinmania.ramahttpbackend.server

import io.github.kotlinmania.ramacore.ExtensionsMut
import io.github.kotlinmania.ramacore.RamaResult
import io.github.kotlinmania.ramacore.rt.Executor

/**
 * Result type of HTTP server serve operations.
 */
public typealias HttpServeResult = RamaResult<Unit, Throwable>

/**
 * A utility interface to allow any of the HTTP core server builders to be used
 * in the same way to serve an HTTP connection.
 */
public sealed interface HttpCoreConnServer

/**
 * HTTP/1.1 server builder.
 */
public class Http1Builder : HttpCoreConnServer {
    internal suspend fun <IO : ExtensionsMut, S> httpCoreServeConnection(
        stream: IO,
        service: S,
    ): HttpServeResult = RamaResult.ok(Unit)

    public companion object {
        public fun new(): Http1Builder = Http1Builder()
    }
}

/**
 * HTTP/2 server builder.
 */
public class Http2Builder(
    public val executor: Executor = Executor.new(),
) : HttpCoreConnServer {
    internal suspend fun <IO : ExtensionsMut, S> httpCoreServeConnection(
        stream: IO,
        service: S,
    ): HttpServeResult = RamaResult.ok(Unit)

    public companion object {
        public fun new(exec: Executor = Executor.new()): Http2Builder =
            Http2Builder(exec)
    }
}

/**
 * Auto HTTP/1.1 + HTTP/2 server builder.
 */
public class AutoBuilder(
    public val executor: Executor = Executor.new(),
) : HttpCoreConnServer {
    public val http1: Http1Builder = Http1Builder.new()
    public val http2: Http2Builder = Http2Builder.new(executor)

    internal suspend fun <IO : ExtensionsMut, S> httpCoreServeConnection(
        stream: IO,
        service: S,
    ): HttpServeResult = RamaResult.ok(Unit)

    public companion object {
        public fun new(exec: Executor = Executor.new()): AutoBuilder =
            AutoBuilder(exec)
    }
}

internal suspend fun <IO : ExtensionsMut, S> HttpCoreConnServer.httpCoreServeConnection(
    stream: IO,
    service: S,
): HttpServeResult =
    when (this) {
        is Http1Builder -> httpCoreServeConnection(stream, service)
        is Http2Builder -> httpCoreServeConnection(stream, service)
        is AutoBuilder -> httpCoreServeConnection(stream, service)
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
