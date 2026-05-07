---
name: news-crawler
description: Use when working on the mcp-java-news-crawler repository and needing the project-specific workflow, transport model, tool contract, local run commands, or smoke-test steps.
---

# News Crawler

## Overview

Project skill for this repository. Use it when changing MCP transports, tool behavior, crawler-facing contracts, or local debug flow.

## Workflow

1. Read `references/transports.md` before changing server startup or protocol handling.
2. Read `references/tool-contract.md` before changing `initialize`, `tools/list`, `tools/call`, `ping`, or tool error behavior.
3. Use `scripts/run-stdio.sh` for local STDIO startup.
4. Use `scripts/run-http.sh` for local Streamable HTTP startup.
5. Use `scripts/smoke-test-http.sh` after HTTP transport changes.

## Guardrails

- Default transport is `stdio`.
- Supported transport modes are `stdio`, `http`, and `both`.
- HTTP v1 endpoint is `POST /mcp`.
- Notification requests return `202`.
- `GET` and `DELETE` are intentionally rejected in HTTP v1.
- Do not change JSON-RPC response shape without updating tests and references together.
- Use JDK 21 for build and test commands in this repository.

## References

- Transport model: `references/transports.md`
- MCP tool contract: `references/tool-contract.md`
- Example commands and payloads: `references/examples.md`

## Local Commands

- STDIO: `./.agents/skills/news-crawler/scripts/run-stdio.sh`
- HTTP: `./.agents/skills/news-crawler/scripts/run-http.sh`
- HTTP smoke test: `./.agents/skills/news-crawler/scripts/smoke-test-http.sh`
