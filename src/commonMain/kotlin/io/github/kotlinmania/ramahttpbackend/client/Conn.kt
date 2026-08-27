// port-lint: source client/conn.rs
package io.github.kotlinmania.ramahttpbackend.client

import io.github.kotlinmania.ramahttpbackend.Body
import io.github.kotlinmania.ramahttpbackend.EstablishedClientConnection
import io.github.kotlinmania.ramahttpbackend.Layer
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Response
import io.github.kotlinmania.ramahttpbackend.Service
import io.github.kotlinmania.ramahttpbackend.Version

/**
 * A [Service] which establishes an HTTP connection.
 */
public class HttpConnector(
    public val inner: Any = Any(),
) : Service<Request, EstablishedClientConnection<HttpClientService<Body>, Request>, Throwable> {

    /**
     * Get mutable reference to inner service.
     */
    public fun innerMut(): Any = inner

    override suspend fun serve(
        input: Request,
    ): RamaResult<EstablishedClientConnection<HttpClientService<Body>, Request>, Throwable> {
        val clientService = when (input.version) {
            Version.HTTP_2 -> HttpClientService.http2<Body>()
            else -> HttpClientService.http1<Body>()
        }
        return RamaResult.ok(EstablishedClientConnection(clientService, input))
    }

    public companion object {
        /**
         * Create a new [HttpConnector].
         */
        public fun new(inner: Any = Any()): HttpConnector = HttpConnector(inner)
    }
}

/**
 * A [Layer] that produces an [HttpConnector].
 */
public class HttpConnectorLayer : Layer<Any, HttpConnector> {

    override fun layer(inner: Any): HttpConnector = HttpConnector(inner)
    override fun intoLayer(inner: Any): HttpConnector = layer(inner)

    public companion object {
        /**
         * Create a new [HttpConnectorLayer].
         */
        public fun new(): HttpConnectorLayer = HttpConnectorLayer()

        /**
         * Create a default [HttpConnectorLayer].
         */
        public fun default(): HttpConnectorLayer = HttpConnectorLayer()
    }
}
