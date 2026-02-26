## ADDED Requirements

### Requirement: Central platform descriptor source
The system SHALL provide a centralized platform registry that is the authoritative source for platform metadata, including canonical ID, display name, aliases, enabled flag, and priority.

#### Scenario: Resolve platform from alias
- **WHEN** a caller provides a platform alias or canonical ID
- **THEN** the registry returns the canonical platform descriptor for that input

### Requirement: Enabled and priority-based selection
The system SHALL expose registry APIs to list enabled platforms and return them in deterministic priority order.

#### Scenario: Retrieve enabled platforms in order
- **WHEN** a caller requests enabled platforms
- **THEN** the registry returns only enabled entries sorted by descending priority and stable ID tie-breaker

### Requirement: Default platform subset
The system SHALL expose a default platform list derived from enabled platforms and default platform count configuration.

#### Scenario: Default count configured
- **WHEN** default platform count is greater than zero
- **THEN** the registry returns only the top N enabled platforms by priority

#### Scenario: Default count unbounded
- **WHEN** default platform count is zero or absent
- **THEN** the registry returns all enabled platforms by priority
