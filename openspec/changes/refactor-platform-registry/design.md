## Context

Current platform-related behavior is split across `PlatformConfig` static registration, `platforms.yml` priority settings, `NewsService.initCrawlers()`, and test-time switch/crawler lists. The same platform IDs and aliases are maintained in multiple places, causing drift and high maintenance cost.

## Goals / Non-Goals

**Goals:**
- Define one authoritative registry for platform metadata and resolution.
- Define one authoritative registry for crawler wiring and lookup.
- Keep existing MCP tool behavior and output contract unchanged.
- Make subsequent refactor phases (error model and service split) easier to execute.

**Non-Goals:**
- No crawler parsing logic changes.
- No MCP tool schema/response changes.
- No ranking strategy or dedupe algorithm changes.

## Decisions

### Decision 1: Introduce `PlatformDescriptor` and `PlatformRegistry`
- `PlatformDescriptor` is an immutable model containing id, display name, aliases, enabled, priority, and category.
- `PlatformRegistry` provides read APIs: resolve alias/id, list enabled platforms, list default platforms, and stable sorting.
- Rationale: replaces scattered static maps and ad-hoc sorting logic with a single domain entry point.
- Alternative considered: keep `PlatformConfig` static maps and only add helper methods. Rejected because duplicated responsibilities remain.

### Decision 2: Introduce `CrawlerRegistry`
- Central map of `platformId -> crawler instance` (or supplier), with lookup and supported ID listing.
- `NewsService` pulls crawlers from this registry rather than hardcoded `addCrawler(...)`.
- Rationale: removes repeated platform wiring in service and tests.
- Alternative considered: use reflection/classpath scanning. Rejected for now to keep startup deterministic and simple.

### Decision 3: Compatibility bridge for current call sites
- Existing `PlatformConfig` APIs may delegate internally to the new registry during migration.
- Rationale: minimize blast radius and allow incremental updates.

## Risks / Trade-offs

- [Risk] Registry bootstrap order issues (config loaded after registry init) -> Mitigation: initialize from explicit constructor flow and add startup validation logs.
- [Risk] Existing IDs/aliases diverge during migration -> Mitigation: add consistency checks and unit tests for all known platform IDs.
- [Trade-off] Temporary duplication while compatibility bridge exists -> Mitigation: remove bridge in later cleanup phase.

## Migration Plan

1. Add new registry/domain classes without changing tool/service behavior.
2. Switch `NewsService` crawler and platform resolution to registries.
3. Route legacy `PlatformConfig` methods to the registry (compatibility layer).
4. Update crawler tests to fetch crawlers from registry.
5. Validate unchanged MCP outputs and archive change when stable.

Rollback:
- Revert service wiring commit; old static platform and hardcoded crawler initialization remain available until bridge removal.

## Open Questions

- Should crawler instances be singleton objects or suppliers creating new instances?
- Should platform category (`domestic/international`) live in descriptor now or later phase?
- Should default platform count remain only in `platforms.yml` or be overridable via environment variable?
