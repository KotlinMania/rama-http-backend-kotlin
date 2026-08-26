# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 0/20 (0.0%)
- **Function parity:** 0/94 matched — 0.0%
- **Class/type parity:** 0/40 matched — 0.0%
- **Combined symbol parity:** 0/134 matched — 0.0%
- **Average inline-code cosine:** 0.00 (function body across 0 matched files)
- **Average documentation cosine:** 0.00 (doc text across 0 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 0 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `client.mod` | `client.Mod` | 0 | `client/mod.rs` | `client/Mod.kt` |
| `client.proxy.layer.mod` | `client.proxy.layer.Mod` | 0 | `client/proxy/layer/mod.rs` | `client/proxy/layer/Mod.kt` |
| `proxy_connector.mod` | `client.proxy.layer.proxyconnector.Mod` | 0 | `client/proxy/layer/proxy_connector/mod.rs` | `client/proxy/layer/proxyconnector/Mod.kt` |
| `proxy.mod` | `client.proxy.Mod` | 0 | `client/proxy/mod.rs` | `client/proxy/Mod.kt` |
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |
| `layer.mod` | `server.layer.Mod` | 0 | `server/layer/mod.rs` | `server/layer/Mod.kt` |
| `upgrade.mod` | `server.layer.upgrade.Mod` | 0 | `server/layer/upgrade/mod.rs` | `server/layer/upgrade/Mod.kt` |
| `server.mod` | `server.Mod` | 0 | `server/mod.rs` | `server/Mod.kt` |

