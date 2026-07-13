# Charles Schwab Event Ledger Submissions

This repository contains two cleanly separated Event Ledger applications.

| Application | Purpose | Location |
|---|---|---|
| Event Ledger | Stable working solution for the original distributed-systems assignment | [event-ledger](event-ledger) |
| AI-Augmented Event Ledger | Separate version that demonstrates AI-assisted SDLC practices with Design, Development, and QA Agent deliverables | [ai-augmented-event-ledger](ai-augmented-event-ledger) |

## Event Ledger

The baseline solution includes two independently runnable Spring Boot microservices, separate H2 databases, REST communication, idempotency, out-of-order event handling, resiliency, tracing, structured logs, metrics, Docker Compose, and automated tests.

```bash
cd event-ledger
mvn clean verify
docker compose up --build
```

## AI-Augmented Event Ledger

The AI-augmented solution keeps the same core distributed-system behavior and adds explicit SDLC evidence:

- Design Agent deliverable
- Development Agent deliverable
- QA Agent deliverable
- Gateway audit trail endpoint: `GET /events/{eventId}/audit`

```bash
cd ai-augmented-event-ledger
mvn clean verify
docker compose up --build
```

Each application has its own `README.md`, `API_CONTRACT.md`, Maven build, Docker files, and service modules.
