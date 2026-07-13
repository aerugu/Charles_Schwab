# AI-Assisted SDLC Traceability

## Purpose

This document explains how AI-assisted engineering practices are represented in this application. The runtime system remains deterministic Spring Boot microservices. The AI value is demonstrated through repeatable SDLC workflows: architecture analysis, implementation planning, code generation support, QA generation, review, documentation, and release automation.

## Agent Roles Used

| Agent | Primary Contribution | Evidence |
|---|---|---|
| Design Agent | Converted requirements into architecture decisions, diagrams, service boundaries, and production-evolution notes | [agents/design-agent.md](agents/design-agent.md), [docs/DESIGN_AGENT.md](docs/DESIGN_AGENT.md), [docs/adr](docs/adr) |
| Development Agent | Guided implementation of error handling, structured logging, resiliency, audit trail, UI, and Docker integration | [agents/development-agent.md](agents/development-agent.md), [docs/DEVELOPMENT_AGENT.md](docs/DEVELOPMENT_AGENT.md) |
| QA Agent | Converted requirements and risks into automated tests, coverage expectations, and quality gates | [agents/qa-agent.md](agents/qa-agent.md), [docs/QA_AGENT.md](docs/QA_AGENT.md), [scripts/run-quality-gates.sh](scripts/run-quality-gates.sh) |
| Review Agent | Provides repeatable review criteria for architecture, security, resiliency, API contract, and documentation | [agents/review-agent.md](agents/review-agent.md), [prompts/security-resiliency-review-prompt.md](prompts/security-resiliency-review-prompt.md) |
| Release Agent | Packages quality evidence and reviewer guidance into a repeatable handoff | [agents/release-agent.md](agents/release-agent.md), [REVIEWER_PATH.md](REVIEWER_PATH.md) |

## AI Tools And Techniques Represented

- AI coding assistant for implementation planning, code generation support, and refactoring review.
- Agent-style playbooks for repeatable design, development, QA, review, and release tasks.
- Prompt templates for architecture review, API contract review, test generation, security/resiliency review, and documentation review.
- Automation scripts for quality gates, coverage summaries, API contract validation, and demo flow execution.
- CI workflow for repeatable backend tests, frontend build, functional tests, Docker Compose validation, and coverage artifact publication.

## Human Review Checkpoints

AI-assisted output is treated as a productivity accelerator, not an authority. Human review checkpoints are built into the workflow:

1. Architecture decisions are documented through ADRs before implementation is considered complete.
2. Code changes must compile and pass automated tests.
3. API contract changes must be validated against controllers and documentation.
4. Security and resiliency recommendations require human acceptance before implementation.
5. Release readiness requires quality-gate evidence and a clean repository state.

## How AI Accelerated Delivery

| SDLC Stage | Acceleration |
|---|---|
| Design | Requirements were decomposed into service boundaries, architecture diagrams, and ADRs quickly. |
| Development | Implementation tasks were scoped around clear patterns: repository ownership, controller contracts, audit entries, resiliency wrappers, and React UI screens. |
| QA | Test scenarios were mapped directly to requirements and failure modes. |
| Documentation | Reviewer-facing docs, agent playbooks, prompt templates, and runbooks were generated consistently. |
| Release | Scripts and CI encode repeatable quality checks instead of relying on manual memory. |

## Manual vs Automated Validation

| Validation | Automated | Manual |
|---|---:|---:|
| Java unit tests | Yes, `mvn test` | Review failures and coverage gaps |
| Java functional tests | Yes, `mvn clean verify` | Inspect degraded-flow behavior and logs |
| Frontend build | Yes, `npm run build` in CI | Review UI workflow and copy |
| API contract references | Yes, `scripts/validate-api-contract.sh` | Confirm endpoint semantics remain accurate |
| Coverage summary | Yes, `scripts/generate-coverage-summary.sh` | Decide whether risk warrants more tests |
| Architecture decisions | No | Human architect review |
| Production readiness | No | Human risk and controls review |

## Runtime AI Clarification

This application is not a runtime LLM-agent system. It does not call an LLM during event processing, account updates, or UI workflows. The AI-augmented aspect is the engineering process: repeatable agent-style workflows and automation are used to improve productivity, quality, and delivery outcomes while keeping financial transaction behavior deterministic and testable.
