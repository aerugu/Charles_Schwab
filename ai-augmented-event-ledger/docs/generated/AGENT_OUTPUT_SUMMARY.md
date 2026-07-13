# Agent Output Summary

This file summarizes representative outputs from the AI-assisted SDLC workflow. It is intentionally checked in as review evidence; runtime-generated build reports are produced by scripts and CI.

| Agent | Representative Output | Evidence |
|---|---|---|
| Design Agent | Service boundary diagram, degraded-flow diagram, ADRs | `docs/DESIGN_AGENT.md`, `docs/adr/*` |
| Development Agent | Audit endpoint, React operations console, structured error handling notes | `docs/DEVELOPMENT_AGENT.md`, source code |
| QA Agent | Requirement-to-test mapping and coverage report paths | `docs/QA_AGENT.md` |
| Review Agent | Security/resiliency review prompt and checklist | `agents/review-agent.md`, `prompts/security-resiliency-review-prompt.md` |
| Release Agent | Evaluation path and quality-gate commands | `agents/release-agent.md`, `EVALUATION_PATH.md` |

## Human Decisions Captured

- Keep runtime behavior deterministic; do not call LLMs in transaction flow.
- Keep Account Service internal to Docker Compose.
- Use Gateway-owned audit entries for public API lifecycle decisions.
- Use scripts and CI to make quality evidence repeatable.
