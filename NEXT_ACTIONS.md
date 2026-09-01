# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 20/20 (100.0%)
- **Function parity:** 61/79 matched (target 137) — 77.2%
- **Class/type parity:** 19/41 matched (target 42) — 46.3%
- **Combined symbol parity:** 80/120 matched (target 179) — 66.7%
- **Average inline-code cosine:** 0.47 (function body across 12 matched files)
- **Average documentation cosine:** 0.69 (doc text across 12 matched files)
- **Cheat-zeroed Files:** 7
- **Critical Issues:** 18 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. server.service

- **Target:** `server.Service`
- **Similarity:** 0.45
- **Dependents:** 1
- **Priority Score:** 1061705.5
- **Functions:** 9/13 matched
- **Missing functions:** `http1_mut`, `h2_mut`, `fmt`, `clone`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Output`, `Error`

### 2. upgrade.layer

- **Target:** `upgrade.Layer`
- **Similarity:** 0.42
- **Dependents:** 1
- **Priority Score:** 1030805.8
- **Functions:** 4/6 matched (target 4)
- **Missing functions:** `fmt`, `clone`
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Service`

### 3. proxy_connector.service

- **Target:** `proxyconnector.Service`
- **Similarity:** 0.32
- **Dependents:** 0
- **Priority Score:** 112306.8
- **Functions:** 10/18 matched (target 20)
- **Missing functions:** `fmt`, `extensions`, `poll_write`, `poll_flush`, `poll_shutdown`, `is_write_vectored`, `poll_write_vectored`, `poll_read`
- **Types:** 2/5 matched (target 3)
- **Missing types:** `Output`, `Error`, `Target`

### 4. client.svc

- **Target:** `client.Svc`
- **Similarity:** 0.64
- **Dependents:** 0
- **Priority Score:** 41403.6
- **Functions:** 8/10 matched
- **Missing functions:** `fmt`, `extensions`
- **Types:** 2/4 matched (target 5)
- **Missing types:** `Output`, `Error`
- **Tests:** 5/5 matched

### 5. upgrade.service

- **Target:** `upgrade.Service`
- **Similarity:** 0.32
- **Dependents:** 0
- **Priority Score:** 40806.8
- **Functions:** 2/4 matched
- **Missing functions:** `fmt`, `clone`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `Output`, `Error`

### 6. layer.proxy_address

- **Target:** `layer.ProxyAddress`
- **Similarity:** 0.61
- **Dependents:** 0
- **Priority Score:** 31203.9
- **Functions:** 7/7 matched (target 18)
- **Missing functions:** _none_
- **Types:** 2/5 matched (target 2)
- **Missing types:** `Service`, `Output`, `Error`

### 7. client.conn

- **Target:** `client.Conn`
- **Similarity:** 0.50
- **Dependents:** 0
- **Priority Score:** 30905.0
- **Functions:** 4/4 matched (target 7)
- **Missing functions:** _none_
- **Types:** 2/5 matched (target 2)
- **Missing types:** `Output`, `Error`, `Service`

### 8. layer.proxy_auth_header

- **Target:** `layer.ProxyAuthHeader`
- **Similarity:** 0.51
- **Dependents:** 0
- **Priority Score:** 30804.9
- **Functions:** 3/3 matched (target 6)
- **Missing functions:** _none_
- **Types:** 2/5 matched (target 2)
- **Missing types:** `Service`, `Output`, `Error`

### 9. server.core_conn

- **Target:** `server.CoreConn`
- **Similarity:** 0.40
- **Dependents:** 0
- **Priority Score:** 10606.0
- **Functions:** 4/4 matched (target 9)
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 5)
- **Missing types:** `Sealed`

### 10. proxy_connector.layer

- **Target:** `proxyconnector.Layer`
- **Similarity:** 0.40
- **Dependents:** 0
- **Priority Score:** 10506.0
- **Functions:** 3/3 matched (target 11)
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Service`

### 11. server.mod

- **Target:** `server.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10110.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/1 matched
- **Missing types:** `HttpServeResult`

### 12. proxy_connector.connector

- **Target:** `proxyconnector.Connector`
- **Similarity:** 0.57
- **Dependents:** 0
- **Priority Score:** 504.3
- **Functions:** 4/4 matched (target 16)
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 13. proxy_connector.proxy_error

- **Target:** `proxyconnector.ProxyError`
- **Similarity:** 0.50
- **Dependents:** 0
- **Priority Score:** 405.0
- **Functions:** 3/3 matched (target 19)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 9)
- **Missing types:** _none_

### 14. client.mod

- **Target:** `client.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 15. layer.mod

- **Target:** `layer.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 16. proxy.mod

- **Target:** `proxy.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 17. rama-http-backend.client.proxy.layer.mod

- **Target:** `commonMain.kotlin.io.github.kotlinmania.ramahttpbackend.client.proxy.layer.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 18. upgrade.mod

- **Target:** `upgrade.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 19. proxy_connector.mod

- **Target:** `proxyconnector.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

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

### Matched

| Source | Target | Path |
|--------|--------|------|
| `rama-http-backend.lib` | `ramahttpbackend.Lib` | `rama-http-backend/src/lib` |

