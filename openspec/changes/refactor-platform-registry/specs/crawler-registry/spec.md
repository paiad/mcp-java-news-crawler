## ADDED Requirements

### Requirement: Central crawler registration
The system SHALL provide a centralized crawler registry mapping canonical platform IDs to crawler implementations.

#### Scenario: Lookup crawler by platform ID
- **WHEN** a caller requests crawler for a known platform ID
- **THEN** the registry returns the corresponding crawler implementation

### Requirement: Supported platform discovery
The system SHALL provide an API to enumerate all platform IDs that have registered crawler implementations.

#### Scenario: List supported crawler platforms
- **WHEN** a caller requests supported platform IDs
- **THEN** the registry returns a deterministic set/list of registered platform IDs

### Requirement: Service integration without behavior change
The system SHALL allow service-layer platform crawling flows to use crawler registry lookups without changing MCP tool response shape for this phase.

#### Scenario: Existing tool call path
- **WHEN** get hot news or search news APIs execute via service layer
- **THEN** response fields and semantics remain unchanged compared to pre-refactor behavior
