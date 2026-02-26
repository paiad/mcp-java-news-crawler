## Why

Platform metadata, alias resolution, enable/priority settings, and crawler registration are duplicated across config, service, and test code. This increases change risk and makes adding a new platform error-prone.

## What Changes

- Introduce a centralized platform registry as the single source of truth for platform metadata and alias resolution.
- Introduce a crawler registry to centralize crawler wiring and supported platform discovery.
- Refactor service-level platform selection to read from registries instead of hardcoded lists.
- Keep tool API behavior unchanged for this phase.

## Capabilities

### New Capabilities
- `platform-registry`: Centralized platform descriptors, alias resolution, enabled filtering, and priority ordering.
- `crawler-registry`: Centralized crawler registration and lookup by platform ID.

### Modified Capabilities
- None.

## Impact

- Affected code: `config`, `service`, `crawler`, and crawler test wiring.
- No external API changes to MCP tools in this phase.
- Reduced platform onboarding footprint for future crawlers.
