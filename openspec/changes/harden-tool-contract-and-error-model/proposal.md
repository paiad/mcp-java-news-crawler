## Why

Current crawler/service/tool boundaries blur failure semantics and response contracts, which makes MCP consumers misinterpret partial failures as success and reduces maintainability. This change hardens reliability and output consistency before further feature work.

## What Changes

- Define a structured per-platform crawl outcome model to separate empty data from execution failures.
- Define a stable MCP tool response contract for full success, partial success, and failure cases.
- Align HTTP connection behavior with shared client pooling and introduce configurable timeout/retry policy by platform group.
- Reduce platform onboarding drift by clarifying a single metadata registration source and crawler registration responsibilities.
- Add output fields needed by consumers (`url`, optional `publishedAt`) with backward-compatible semantics.

## Capabilities

### New Capabilities
- `crawl-outcome-model`: Structured per-platform crawl execution outcomes and aggregation rules.
- `tool-response-contract`: Normative response shape and semantics for MCP tools.
- `http-connection-policy`: Connection/retry/timeout policy compatible with shared client pooling.

### Modified Capabilities
- `platform-registry`: Tighten requirements so platform metadata and crawler support are validated for consistency.

## Impact

- Affected code: `crawler`, `service`, `tool`, `model`, `registry`.
- MCP tool consumers get clearer partial-failure semantics and richer item fields.
- Tests need expansion for contract and failure-path scenarios.
