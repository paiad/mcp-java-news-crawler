## 1. Outcome Model

- [x] 1.1 Add `PlatformCrawlOutcome` model and status enum
- [x] 1.2 Refactor crawler execution path to return structured outcomes
- [x] 1.3 Update aggregate crawl result construction to use structured outcomes

## 2. Tool Contract

- [x] 2.1 Define and document `success` and `partial_success` semantics
- [x] 2.2 Update tool response builders to include structured failure diagnostics
- [x] 2.3 Extend `NewsItemVO` with `url` and optional `publishedAt`

## 3. HTTP Policy Alignment

- [x] 3.1 Remove forced connection-close request behavior
- [x] 3.2 Define timeout/retry defaults by platform group
- [x] 3.3 Ensure timeout paths map to timeout outcome status

## 4. Registry Consistency

- [x] 4.1 Add startup consistency checks between enabled platforms and crawler registry
- [x] 4.2 Add configurable strict/degraded validation mode
- [x] 4.3 Add tests for missing registration and deterministic warnings/errors

## 5. Validation

- [x] 5.1 Add unit tests for partial success and failure mapping
- [x] 5.2 Add tool contract regression tests for response shape compatibility
- [x] 5.3 Run `mvn test` and `openspec validate harden-tool-contract-and-error-model`
