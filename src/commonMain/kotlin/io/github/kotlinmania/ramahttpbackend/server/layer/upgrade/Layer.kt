// port-lint: source server/layer/upgrade/layer.rs
package io.github.kotlinmania.ramahttpbackend.server.layer.upgrade

import io.github.kotlinmania.ramahttpbackend.Layer
import io.github.kotlinmania.ramahttpbackend.Matcher
import io.github.kotlinmania.ramahttpbackend.Request
import io.github.kotlinmania.ramahttpbackend.Service
import io.github.kotlinmania.ramahttpbackend.Upgraded

/**
 * Middleware that can be used to upgrade a request.
 */
internal class UpgradeLayer<O : Any>(
    public val handlers: MutableList<UpgradeHandler<O>> = mutableListOf(),
) : Layer<Service<Request, O, Nothing>, UpgradeService<O, Nothing>> {
    /**
     * Add an extra upgrade handler to the layer.
     */
    public fun on(
        matcher: Matcher<Request>,
        responder: Service<Request, Pair<O, Request>, O>,
        handler: Service<Upgraded, Unit, Nothing>,
    ): UpgradeLayer<O> {
        handlers.add(UpgradeHandler.new(matcher, responder, handler))
        return this
    }

    override fun layer(inner: Service<Request, O, Nothing>): UpgradeService<O, Nothing> =
        UpgradeService.new(handlers.toList(), inner)

    override fun intoLayer(inner: Service<Request, O, Nothing>): UpgradeService<O, Nothing> =
        layer(inner)

    public companion object {
        /**
         * Create a new upgrade layer.
         */
        public fun <O : Any> new(
            matcher: Matcher<Request>,
            responder: Service<Request, Pair<O, Request>, O>,
            handler: Service<Upgraded, Unit, Nothing>,
        ): UpgradeLayer<O> =
            UpgradeLayer(mutableListOf(UpgradeHandler.new(matcher, responder, handler)))
    }
}
