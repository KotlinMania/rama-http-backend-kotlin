// port-lint: source client/proxy/layer/proxy_connector/mod.rs
package io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector

// Module tracking ledger for the proxy_connector module.
// Upstream mod.rs re-exports symbols from submodules; the Kotlin
// equivalents live in their own files in this package.
//
// mod connector;
// internal usage only
// use connector::InnerHttpProxyConnector;
//
// mod proxy_error;
// pub use proxy_error::HttpProxyError;
//
// mod layer;
// pub use layer::HttpProxyConnectorLayer;
//
// mod service;
// pub use service::{
//     HttpProxyConnectResponseHeaders, HttpProxyConnector, MaybeHttpProxiedConnection,
// };

/**
 * Marker type for the [proxy_connector] module boundary.
 * The upstream Rust `mod.rs` is purely declarative (module declarations
 * and re-exports). In Kotlin the individual symbols live in their own
 * files within this package; this object exists so the module-tracking
 * ledger file is not empty.
 */
internal object ProxyConnectorModule
