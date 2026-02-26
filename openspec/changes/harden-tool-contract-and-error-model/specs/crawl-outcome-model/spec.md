## ADDED Requirements

### Requirement: Platform crawl outcomes are explicit
The system SHALL represent each platform crawl attempt as a structured outcome with status, latency, items, and optional error details.

#### Scenario: Successful platform crawl
- **WHEN** a crawler returns one or more items without exception
- **THEN** the outcome status is `SUCCESS` and includes items and latency

#### Scenario: Empty but valid platform crawl
- **WHEN** a crawler completes without exception and returns zero items
- **THEN** the outcome status is `EMPTY` and includes latency without failure error code

#### Scenario: Failed platform crawl
- **WHEN** a crawler throws an exception during execution
- **THEN** the outcome status is `FAILED` (or `TIMEOUT` where applicable) and includes normalized error fields

### Requirement: Aggregate results preserve failure fidelity
The system SHALL aggregate platform outcomes into overall crawl results without inferring failure from empty item lists.

#### Scenario: Mixed platform outcomes
- **WHEN** some platforms succeed and at least one platform fails
- **THEN** the aggregate result includes successful items and structured failure details for failed platforms
