## Context

The current implementation aggregates crawler outputs by collecting item lists and optional failure messages, but crawler-level exceptions are often collapsed into empty lists. Tool responses always carry `success=true` unless execution throws, which weakens diagnostics for MCP clients.

## Goals / Non-Goals

**Goals:**
- Introduce explicit per-platform crawl outcome types and preserve error details.
- Standardize tool response semantics for complete, partial, and failed crawl states.
- Ensure HTTP connection usage benefits from shared OkHttp pooling.
- Keep migration incremental and backward-compatible where possible.

**Non-Goals:**
- No major parser rewrites for individual crawler implementations.
- No MCP transport/protocol changes.

## Decisions

### Decision 1: Introduce platform-level outcome object
- Add `PlatformCrawlOutcome` with fields: `platformId`, `status`, `items`, `errorCode`, `errorMessage`, `latencyMs`.
- Status enum: `SUCCESS`, `EMPTY`, `FAILED`, `TIMEOUT`.
- Service aggregation derives `failures` from structured outcomes, not inferred empty lists.

### Decision 2: Define response contract levels
- Tool-level `success` indicates tool execution success only.
- Add explicit `partial_success` boolean and normalized `failures` array/object.
- Preserve existing top-level `data` and `count`; add `meta` for diagnostics.

### Decision 3: Enforce connection policy alignment
- Remove request header forcing connection close.
- Keep retry on network failures only; add group-level timeout policy inputs (domestic/international defaults).

### Decision 4: Registry consistency checks
- Add validation that enabled platforms with default selection have crawler registrations.
- Fail fast at startup (or log clear warnings with degraded mode toggle).

## Risks / Trade-offs

- [Risk] Existing clients rely on old `success` meaning all platforms succeeded -> Mitigation: document semantics and add `partial_success`.
- [Risk] More verbose error payloads increase response size -> Mitigation: keep compact fields and optional detail mode.
- [Trade-off] Strong startup validation can block startup in misconfigured environments -> Mitigation: configurable strict mode.

## Migration Plan

1. Add outcome model and adapt service internals.
2. Keep old response fields, add new semantics fields (`partial_success`, structured failures).
3. Update `NewsItemVO` with `url` and optional `publishedAt`.
4. Update tests and OpenSpec artifacts.

Rollback:
- Maintain compatibility adapter mapping outcomes back to previous `CrawlResult` fields during rollout.

## Open Questions

- Should failure diagnostics be in map form (`platform -> reason`) or array entries with typed codes?
- Should `publishedAt` be raw string, epoch millis, or both?
