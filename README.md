# Event Ledger Applications

This repository contains two cleanly separated Event Ledger applications. They share the same core financial event-ledger domain, but they serve different review purposes.

| Application | Purpose | Location |
|---|---|---|
| Event Ledger | Baseline distributed-systems implementation | [event-ledger](event-ledger) |
| AI-Augmented Event Ledger | Extended implementation that demonstrates AI-assisted SDLC practices, auditing, and a React operations UI | [ai-augmented-event-ledger](ai-augmented-event-ledger) |

## Reviewer Quick Start

For the most complete review path, start with the AI-augmented implementation:

```bash
cd ai-augmented-event-ledger
./scripts/run-quality-gates.sh
docker compose up --build
```

Then open the React operations console:

```text
http://localhost:3000
```

The AI-assisted SDLC evidence is intentionally easy to inspect:

| Reviewer Question | Where To Look |
|---|---|
| How was AI-assisted engineering applied across the SDLC? | [ai-augmented-event-ledger/AI_ASSISTED_SDLC.md](ai-augmented-event-ledger/AI_ASSISTED_SDLC.md) |
| What is the step-by-step reviewer path? | [ai-augmented-event-ledger/REVIEWER_PATH.md](ai-augmented-event-ledger/REVIEWER_PATH.md) |
| What agent workflows were used? | [ai-augmented-event-ledger/agents](ai-augmented-event-ledger/agents) |
| What reusable prompts support repeatability? | [ai-augmented-event-ledger/prompts](ai-augmented-event-ledger/prompts) |
| What architecture decisions were made? | [ai-augmented-event-ledger/docs/adr](ai-augmented-event-ledger/docs/adr) |
| What automation validates quality? | [ai-augmented-event-ledger/scripts](ai-augmented-event-ledger/scripts) |
| What generated evidence summarizes agent output and quality gates? | [ai-augmented-event-ledger/docs/generated](ai-augmented-event-ledger/docs/generated) |
| What does CI validate? | [.github/workflows/ai-augmented-event-ledger-ci.yml](.github/workflows/ai-augmented-event-ledger-ci.yml) |

## Why Two Applications?

The `event-ledger` application is the clean baseline implementation of the Event Gateway and Account Service architecture. It focuses on the required distributed-systems behavior: idempotency, out-of-order event handling, independent service databases, synchronous REST communication, resiliency, trace propagation, structured logging, metrics, Docker Compose, and automated tests.

The `ai-augmented-event-ledger` application keeps that same deterministic backend architecture and adds visible SDLC evidence for how AI-assisted engineering practices can support delivery. It includes Design, Development, QA, Review, and Release Agent playbooks; reusable prompt templates; architecture decision records; automation scripts; generated quality evidence; a GitHub Actions CI workflow; and additional product capabilities such as a Gateway audit trail and a React-based operations console.

The AI-augmented application is not a runtime LLM-agent system. It does not execute autonomous AI agents inside the production application flow. Instead, it demonstrates an AI-augmented engineering workflow: agent-style roles are represented through structured design, development, and QA deliverables, while the application itself remains a deterministic Spring Boot microservices system.

## Capability Comparison

| Capability | `event-ledger` | `ai-augmented-event-ledger` |
|---|---:|---:|
| Event Gateway and Account Service microservices | Yes | Yes |
| Separate embedded H2 database per service | Yes | Yes |
| Idempotent event submission | Yes | Yes |
| Out-of-order event listing | Yes | Yes |
| Balance calculation through Account Service | Yes | Yes |
| Timeout, retry, jitter, circuit breaker | Yes | Yes |
| Pending outbox fallback | Yes | Yes |
| Trace ID propagation and JSON logs | Yes | Yes |
| Health and metrics endpoints | Yes | Yes |
| Gateway audit trail endpoint | No | Yes |
| React operations console | No | Yes |
| Design, Development, QA, Review, and Release Agent playbooks | No | Yes |
| Reusable AI prompt templates | No | Yes |
| Architecture decision records | No | Yes |
| Reviewer quality-gate scripts | No | Yes |
| GitHub Actions CI workflow | No | Yes |

## Event Ledger

The baseline solution includes two independently runnable Spring Boot microservices, separate H2 databases, REST communication, idempotency, out-of-order event handling, resiliency, tracing, structured logs, metrics, Docker Compose, and automated tests.

```bash
cd event-ledger
mvn clean verify
docker compose up --build
```

## AI-Augmented Event Ledger

The AI-augmented solution keeps the same core distributed-system behavior and adds explicit SDLC evidence:

- Design Agent playbook: architecture decisions, diagrams, risks, and reviewer-ready design outputs
- Development Agent playbook: implementation, logging, error handling, resiliency, auditing, and commit discipline
- QA Agent playbook: unit tests, functional tests, coverage reports, and acceptance criteria
- Review Agent playbook: repeatable architecture, API contract, resiliency, security, and documentation review
- Release Agent playbook: reviewer handoff, quality evidence, CI expectations, and demo readiness
- Prompt templates: architecture review, API contract review, test generation, security/resiliency review, and documentation improvement
- Automation scripts: quality gates, API contract validation, coverage summary, and demo flow
- CI workflow: backend build, unit tests, functional tests, React build, coverage artifacts, and Docker Compose validation
- Gateway audit trail endpoint: `GET /events/{eventId}/audit`
- React operations console

```bash
cd ai-augmented-event-ledger
./scripts/run-quality-gates.sh
docker compose up --build
```

Each application has its own `README.md`, `API_CONTRACT.md`, Maven build, Docker files, and service modules.
