// port-lint: source rama-http-backend/src/server/layer/upgrade/service.rs
package io.github.kotlinmania.ramahttpbackend.server.layer.upgrade

import io.github.kotlinmania.ramahttpbackend.BoxService
import io.github.kotlinmania.ramahttpbackend.Extensions
import io.github.kotlinmania.ramahttpbackend.Matcher
import io.github.kotlinmania.ramahttpbackend.RamaResult
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Service
import io.github.kotlinmania.ramahttpbackend.Upgraded

/**
 * Upgrade service can be used to handle the possibility of upgrading a request,
 * after which it will pass down the transport RW to the attached upgrade service.
 */
internal class UpgradeService<O : Any, E : Any>(
    public val handlers: List<UpgradeHandler<O>>,
    public val inner: Service<Request, O, E>,
) : Service<Request, O, E> {

    /**
     * Get mutable reference to inner service.
     */
    public fun innerMut(): Service<Request, O, E> = inner

    override suspend fun serve(input: Request): RamaResult<O, E> {
        for (handler in handlers) {
            val ext = Extensions.new()
            if (!handler.matcher.matches(ext, input)) {
                continue
            }
            input.extensionsMut().extend(ext)

            return when (val respResult = handler.responder.serve(input)) {
                is RamaResult.Ok -> {
                    val (response, _) = respResult.value
                    RamaResult.ok(response)
                }
                is RamaResult.Err -> RamaResult.ok(respResult.error)
            }
        }
        return inner.serve(input)
    }

    public companion object {
        public fun <O : Any, E : Any> new(
            handlers: List<UpgradeHandler<O>>,
            inner: Service<Request, O, E>,
        ): UpgradeService<O, E> = UpgradeService(handlers, inner)
    }
}

/**
 * Helper class used to handle an upgrade path.
 */
internal class UpgradeHandler<O : Any>(
    public val matcher: Matcher<Request>,
    public val responder: BoxService<Request, Pair<O, Request>, O>,
    public val handler: BoxService<Upgraded, Unit, Nothing>,
) {
    public companion object {
        public fun <O : Any> new(
            matcher: Matcher<Request>,
            responder: Service<Request, Pair<O, Request>, O>,
            handler: Service<Upgraded, Unit, Nothing>,
        ): UpgradeHandler<O> =
            UpgradeHandler(matcher, responder.boxed(), handler.boxed())
    }
}
