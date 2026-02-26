## ADDED Requirements

### Requirement: Tool success semantics are explicit
Tool responses SHALL use `success` to indicate tool execution status and SHALL use a separate field to indicate partial platform failures.

#### Scenario: Partial platform failures
- **WHEN** tool execution completes and one or more platforms fail
- **THEN** response includes `success=true`, `partial_success=true`, and structured failure details

#### Scenario: Tool execution failure
- **WHEN** tool execution fails before producing crawl output
- **THEN** response includes `success=false` and an error payload describing the execution failure

### Requirement: Response shape remains backward-compatible
Tool responses SHALL preserve existing top-level data fields while adding diagnostics fields.

#### Scenario: Existing consumer compatibility
- **WHEN** a consumer reads `data`, `count`, and `timestamp`
- **THEN** those fields remain present with unchanged meaning

### Requirement: Output items include actionable links
Tool item outputs SHALL include `url` when available and MAY include normalized publication time fields.

#### Scenario: News item has source link
- **WHEN** a crawled news item contains link information
- **THEN** the corresponding tool output item includes `url`
