## MODIFIED Requirements

### Requirement: Central platform descriptor source
The system SHALL provide a centralized platform registry that is the authoritative source for platform metadata, including canonical ID, display name, aliases, enabled flag, and priority, and SHALL expose consistency validation against crawler registrations for enabled/default-selected platforms.

#### Scenario: Resolve platform from alias
- **WHEN** a caller provides a platform alias or canonical ID
- **THEN** the registry returns the canonical platform descriptor for that input

#### Scenario: Enabled platform missing crawler registration
- **WHEN** startup validation checks enabled or default-selected platforms
- **THEN** the system surfaces a deterministic validation result (strict fail or explicit degraded warning based on configuration)
