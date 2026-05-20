# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 1/20 (5.0%)
- **Function parity:** 2/94 matched (target 3) — 2.1%
- **Class/type parity:** 1/40 matched (target 5) — 2.5%
- **Combined symbol parity:** 3/134 matched (target 8) — 2.2%
- **Average inline-code cosine:** 0.32 (function body across 1 matched files)
- **Average documentation cosine:** 0.95 (doc text across 1 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 1 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. proxy_connector.proxy_error

- **Target:** `proxyconnector.ProxyError`
- **Similarity:** 0.32
- **Dependents:** 0
- **Priority Score:** 10406.8
- **Functions:** 2/3 matched
- **Missing functions:** `fmt`
- **Types:** 1/1 matched (target 5)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../tmp/rama-http-backend/src rust ../../src/commonMain/kotlin/io/github/kotlinmania/ramahttpbackend kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
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

