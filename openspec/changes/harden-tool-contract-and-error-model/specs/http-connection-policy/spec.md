## ADDED Requirements

### Requirement: Shared HTTP client pooling is preserved
Crawler HTTP requests SHALL not force connection closure in a way that disables shared connection pooling.

#### Scenario: Standard GET request
- **WHEN** a crawler executes a GET request via shared OkHttp client
- **THEN** request headers and client behavior allow connection reuse by default

### Requirement: Retry and timeout policy is explicit
The system SHALL define retry and timeout behavior by platform group with deterministic defaults.

#### Scenario: Network transient error
- **WHEN** a request fails with network I/O exception
- **THEN** retry policy is applied according to configured max retries and backoff

#### Scenario: Request timeout reached
- **WHEN** request or platform execution exceeds configured timeout
- **THEN** outcome is marked timeout with corresponding failure code
