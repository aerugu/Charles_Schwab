# Event Ledger

Event Ledger is a two-service Spring Boot system for accepting financial transaction events and applying them to account state. It is designed for duplicate delivery, out-of-order arrival, traceable service-to-service calls, and graceful behavior when the internal account service is unavailable.

## Architecture

```mermaid
flowchart LR
    client["Client / Upstream Systems"]

    subgraph edge["Public Boundary"]
        limiter["Gateway Rate Limiter<br/>Token bucket / 429"]
        gateway["Event Gateway API<br/>Public REST API :8080<br/>Validation, idempotency, tracing"]
    end

    subgraph gatewayStore["Gateway-Owned Persistence"]
        gatewayDb[("Gateway H2 DB<br/>events + pending outbox")]
        retry["Pending Event Retry Worker<br/>scheduled async fallback"]
    end

    subgraph internal["Internal Account Domain"]
        account["Account Service<br/>Internal REST API :8081<br/>balances + transaction history"]
        accountDb[("Account H2 DB<br/>transactions")]
    end

    metrics["Observability<br/>JSON logs, /metrics, /metrics/prometheus"]

    client -->|"POST /events<br/>GET /events<br/>GET /accounts/{id}/balance"| limiter
    limiter -->|"rate-limited traffic"| gateway
    gateway -->|"local reads/writes"| gatewayDb
    gateway -->|"REST + X-Trace-Id<br/>timeout, retry, jitter, circuit breaker"| account
    gateway -->|"queue when Account Service is down<br/>202 Accepted"| gatewayDb
    retry -->|"read due pending events"| gatewayDb
    retry -->|"replay transactions when recovered"| account
    account -->|"local reads/writes"| accountDb
    gateway -.->|"structured logs + custom metrics"| metrics
    account -.->|"structured logs + health + metrics"| metrics

    classDef client fill:#E0F2FE,stroke:#0284C7,color:#0F172A,stroke-width:2px;
    classDef gateway fill:#DCFCE7,stroke:#16A34A,color:#052E16,stroke-width:2px;
    classDef persistence fill:#FEF3C7,stroke:#D97706,color:#451A03,stroke-width:2px;
    classDef worker fill:#FCE7F3,stroke:#DB2777,color:#500724,stroke-width:2px;
    classDef service fill:#EDE9FE,stroke:#7C3AED,color:#2E1065,stroke-width:2px;
    classDef observability fill:#F1F5F9,stroke:#475569,color:#0F172A,stroke-width:2px;

    class client client;
    class limiter,gateway gateway;
    class gatewayDb,accountDb persistence;
    class retry worker;
    class account service;
    class metrics observability;

    style edge fill:#F8FAFC,stroke:#94A3B8,color:#334155,stroke-width:1px,stroke-dasharray: 5 5;
    style gatewayStore fill:#FFFBEB,stroke:#F59E0B,color:#334155,stroke-width:1px,stroke-dasharray: 5 5;
    style internal fill:#F5F3FF,stroke:#8B5CF6,color:#334155,stroke-width:1px,stroke-dasharray: 5 5;
```

The Event Gateway is the public-facing entry point. It validates incoming transaction events, stores accepted events in its own H2 database, enforces `eventId` idempotency, lists events in `eventTimestamp` order, and propagates `X-Trace-Id` to downstream calls.

The Account Service is an internal service called only by the Gateway. It stores applied transactions in its own H2 database, applies duplicate transactions idempotently, computes balance as `CREDIT - DEBIT`, and exposes account details to the Gateway.

The services are independently runnable processes and do not share database state. In Docker Compose, only the Gateway publishes a host port; the Account Service remains internal to the Compose network.

## Setup Instructions

Prerequisites:

- Java 21+
- Maven 3.9+
- Docker and Docker Compose, optional but recommended

Install dependencies and compile all modules:

```bash
mvn clean install -DskipTests
```

## Run With Docker Compose

```bash
docker compose up --build
```

Gateway:

```bash
curl http://localhost:8080/health
```

Account Service:

```bash
docker compose logs account-service
```

In Docker Compose, the Account Service is intentionally not published to the host. It is reachable by the Gateway on the Compose network at `http://account-service:8081`, which keeps the public API boundary aligned with the exercise requirement.

## Run Locally

Start the Account Service:

```bash
mvn -pl account-service -am spring-boot:run
```

Start the Gateway in a second terminal:

```bash
mvn -pl event-gateway -am spring-boot:run
```

Submit an event:

```bash
curl -i -X POST http://localhost:8080/events \
  -H 'Content-Type: application/json' \
  -H 'X-Trace-Id: demo-trace-001' \
  -d '{
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T14:02:11Z",
    "metadata": {
      "source": "mainframe-batch",
      "batchId": "B-9042"
    }
  }'
```

Useful endpoints:

- `POST /events`
- `GET /events/{id}`
- `GET /events?account={accountId}`
- `GET /accounts/{accountId}/balance`
- `GET /accounts/{accountId}`
- `GET /health`
- `GET /metrics`
- `GET /metrics/prometheus`

The explicit Gateway and Account Service HTTP contracts are documented in [API_CONTRACT.md](API_CONTRACT.md).

## QA And Coverage

Run unit tests only:

```bash
mvn test
```

Run the complete automated test suite, including functional tests, real Gateway to Account Service flow, and both coverage report sets:

```bash
mvn clean verify
```

The build uses Surefire for unit tests and Failsafe for functional tests:

- Unit tests cover account balance math, repository-level duplicate event protection, event submission locking, circuit breaker behavior, the Gateway-to-Account consumer contract, and async fallback queue behavior.
- Functional tests cover Gateway and Account Service REST flows, an end-to-end Gateway to real Account Service transaction flow, idempotent duplicate submissions, out-of-order event listing, validation failures, trace propagation, concurrent duplicate failure handling, and graceful Gateway behavior when Account Service is unavailable.

Coverage reports are generated as HTML:

- `account-service/target/site/jacoco-unit/index.html`
- `event-gateway/target/site/jacoco-unit/index.html`
- `account-service/target/site/jacoco-functional/index.html`
- `event-gateway/target/site/jacoco-functional/index.html`

JaCoCo CSV files are emitted next to each HTML report for machine-readable coverage review.

## Resiliency Choice

The Gateway uses timeout + retry with exponential backoff and jitter, plus a small circuit breaker around Account Service calls. Timeout and retry prevent slow or transient failures from hanging client requests. Exponential backoff with jitter avoids synchronized retry bursts when the Account Service is struggling. The circuit breaker opens after repeated failures so balance and account-detail proxy calls can fail fast with `503 Service Unavailable`.

The Gateway serializes submission processing per `eventId`, then claims that `eventId` in its local store before calling the Account Service. That makes the Gateway the source of truth for event identity and prevents concurrent duplicate submissions from racing into inconsistent Gateway and Account records. If the Account Service is unavailable, the Gateway stores the event in a local pending outbox, returns `202 Accepted`, and a scheduled retry worker applies the transaction when the Account Service recovers.

`GET /events/{id}` and `GET /events?account=...` read only from the Gateway database, so they continue working during Account Service outages. Balance and account-detail queries return a clear `503` when the Account Service cannot be reached.

The Gateway also applies a configurable token-bucket rate limiter at the edge. The default configuration allows short bursts while protecting the service from sustained request spikes; excess requests return `429 Too Many Requests`.

## High-Volume And Low-Latency Readiness

This implementation is intentionally scoped to the hiring exercise constraints, but the design choices were made with high-volume financial event ingestion in mind. The Gateway performs deterministic validation and idempotency checks before downstream calls, persists accepted events locally, isolates Account Service failures with timeout, retry, jitter, and circuit breaker behavior, and uses a pending outbox so transaction events are not lost when the internal service is unavailable. Reads for event history stay local to the Gateway, which keeps ledger lookups available even during Account Service degradation.

For the exercise, both services use embedded H2 databases to keep the solution easy to run and review. In a production high-throughput environment, the same service boundaries would be retained while replacing the embedded stores with independently owned production databases, such as PostgreSQL or Aurora, with indexes and partitioning around `accountId`, `eventId`, and `eventTimestamp`. The Gateway idempotency table would be backed by a durable unique constraint and, if needed, accelerated with Redis for hot-key duplicate detection and distributed rate limiting.

The current synchronous REST path is appropriate for the requested architecture and provides simple request-level traceability. For ultra-low-latency and very high-volume ingestion, the next evolution would introduce Kafka, Pulsar, or another durable streaming layer between event acceptance and account projection. In that model, `POST /events` would acknowledge after validation, idempotency claim, and durable enqueue; Account Service consumers would process events asynchronously by account partition, preserving per-account ordering while scaling horizontally. This would reduce client-facing latency and decouple ingestion throughput from account projection throughput.

The implementation already includes several patterns that support this evolution:

- Gateway-owned event ledger and Account-owned transaction state, avoiding shared database coupling.
- Idempotent event and transaction handling, which is required for at-least-once delivery.
- Out-of-order-safe balance calculation based on transaction sums instead of arrival order.
- Local outbox and retry worker, which mirrors the production outbox/streaming pattern at exercise scale.
- Trace propagation, structured logs, health checks, and metrics hooks for operational visibility.
- Rate limiting and circuit breaking to protect the system under spikes or downstream failures.

Before claiming production-grade high-volume, ultra-low-latency readiness, I would add load tests with explicit SLOs, production persistence, distributed idempotency/rate limiting, container orchestration autoscaling, real OpenTelemetry tracing with Jaeger or Zipkin, Prometheus/Grafana alerting, database migration tooling, and capacity testing for hot-account scenarios. The current implementation is therefore production-minded and architecturally prepared for scale, while deliberately keeping infrastructure lightweight for reviewability.

## Observability

Both services emit JSON log lines with:

- `timestamp`
- `level`
- `service`
- `traceId`
- request metadata such as method, path, status, and duration

The Gateway creates a trace ID for each incoming request when the client does not provide `X-Trace-Id`. The same header is propagated to the Account Service and returned in responses.

Both services expose `GET /health`, which returns service status, active database connectivity, and basic diagnostics such as row counts.

Both services expose `GET /metrics`, which returns request and error counts by endpoint template. The Gateway also exposes `GET /metrics/prometheus` for Prometheus-compatible scraping.

This project keeps trace propagation lightweight with `X-Trace-Id` and structured logs. OpenTelemetry Collector plus Jaeger or Zipkin would be a natural next production step, but was not added to avoid turning the exercise into an infrastructure-heavy deployment.

## API Contract

`POST /events` accepts:

```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": {
    "source": "mainframe-batch"
  }
}
```

New events return `201 Created` when Account Service is available. If Account Service is unavailable, the Gateway stores the event locally for retry and returns `202 Accepted`. Duplicate `eventId` submissions return `200 OK` with the original event and `"duplicate": true`; the account balance is not changed again.
