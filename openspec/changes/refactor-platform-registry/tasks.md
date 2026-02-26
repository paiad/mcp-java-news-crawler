## 1. Registry Foundations

- [ ] 1.1 Add `PlatformDescriptor` immutable model for canonical platform metadata
- [ ] 1.2 Implement `PlatformRegistry` with resolve/list/default selection APIs
- [ ] 1.3 Wire `PlatformRegistry` to existing priority configuration source

## 2. Crawler Wiring

- [ ] 2.1 Implement `CrawlerRegistry` with lookup and supported ID enumeration
- [ ] 2.2 Migrate `NewsService` crawler initialization to `CrawlerRegistry`
- [ ] 2.3 Keep backward-compatible behavior for existing `PlatformConfig` call sites

## 3. Service Integration

- [ ] 3.1 Migrate platform resolution in `NewsService` to `PlatformRegistry`
- [ ] 3.2 Preserve current get/search output behavior and limits
- [ ] 3.3 Ensure stable platform ordering and enabled filtering remain intact

## 4. Validation

- [ ] 4.1 Add tests for alias resolution, enabled filtering, and priority ordering
- [ ] 4.2 Add tests for crawler registry supported platform IDs and lookup
- [ ] 4.3 Run build/test and compare MCP tool response shape before/after