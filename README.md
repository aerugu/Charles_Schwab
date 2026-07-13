# Event Ledger Applications

This repository contains two cleanly separated Event Ledger applications. They share the same core financial event-ledger domain, but they serve different review purposes.

| Application | Purpose | Location |
|---|---|---|
| Event Ledger | Baseline distributed-systems implementation | [event-ledger](event-ledger) |
| AI-Augmented Event Ledger | Extended implementation that demonstrates AI-assisted SDLC practices, auditing, and a React operations UI | [ai-augmented-event-ledger](ai-augmented-event-ledger) |

## Why Two Applications?

The `event-ledger` application is the clean baseline implementation of the Event Gateway and Account Service architecture. It focuses on the required distributed-systems behavior: idempotency, out-of-order event handling, independent service databases, synchronous REST communication, resiliency, trace propagation, structured logging, metrics, Docker Compose, and automated tests.

The `ai-augmented-event-ledger` application keeps that same deterministic backend architecture and adds visible SDLC evidence for how AI-assisted engineering practices can support delivery. It includes Design Agent, Development Agent, and QA Agent documentation, plus additional product capabilities such as a Gateway audit trail and a React-based operations console.

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
| Design, Development, and QA Agent SDLC docs | No | Yes |

## Event Ledger

The baseline solution includes two independently runnable Spring Boot microservices, separate H2 databases, REST communication, idempotency, out-of-order event handling, resiliency, tracing, structured logs, metrics, Docker Compose, and automated tests.

```bash
cd event-ledger
mvn clean verify
docker compose up --build
```

## AI-Augmented Event Ledger

The AI-augmented solution keeps the same core distributed-system behavior and adds explicit SDLC evidence:

- Design Agent deliverable: architecture decisions and diagrams
- Development Agent deliverable: implementation, logging, error handling, resiliency, and auditing notes
- QA Agent deliverable: test strategy, coverage report locations, and acceptance criteria
- Gateway audit trail endpoint: `GET /events/{eventId}/audit`
- React operations console

```bash
cd ai-augmented-event-ledger
mvn clean verify
docker compose up --build
```

Each application has its own `README.md`, `API_CONTRACT.md`, Maven build, Docker files, and service modules.
