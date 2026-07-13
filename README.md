# Event Ledger

Event Ledger is a two-service Spring Boot system for accepting financial transaction events and applying them to account state. It is designed for duplicate delivery, out-of-order arrival, traceable service-to-service calls, and graceful behavior when the internal account service is unavailable.

## Architecture

```text
Client
  |
  v
Event Gateway API :8080
  - validates and stores accepted events in its own H2 database
  - enforces eventId idempotency
  - lists account events by eventTimestamp
  - propagates X-Trace-Id to the Account Service
  |
  | synchronous REST with timeout, retry, and circuit breaker
  v
Account Service :8081
  - applies transactions idempotently in its own H2 database
  - computes balance as CREDIT minus DEBIT
  - exposes account details and recent transactions
```

The services are independently runnable processes and do not share database state.

## Requirements

- Java 21+
- Maven 3.9+
- Docker and Docker Compose, optional but recommended

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
curl http://localhost:8081/health
```

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

## Tests

Run all automated tests:

```bash
mvn test
```

Coverage includes:

- Gateway idempotency and duplicate response behavior
- Chronological event listing for out-of-order arrivals
- Validation failures for invalid payloads
- Account balance computation and account-side idempotency
- Trace ID propagation from Gateway to Account Service
- Gateway resiliency when Account Service is down
- Gateway local reads continuing while Account Service is unavailable

## Resiliency Choice

The Gateway uses timeout + retry with linear backoff and a small circuit breaker around Account Service calls. Timeout and retry prevent slow or transient failures from hanging client requests. The circuit breaker opens after repeated failures so the Gateway can fail fast with `503 Service Unavailable` instead of repeatedly spending resources on an unhealthy dependency.

`GET /events/{id}` and `GET /events?account=...` read only from the Gateway database, so they continue working during Account Service outages. Balance and account-detail queries return a clear `503` when the Account Service cannot be reached.

## Observability

Both services emit JSON log lines with:

- `timestamp`
- `level`
- `service`
- `traceId`
- request metadata such as method, path, status, and duration

The Gateway creates a trace ID for each incoming request when the client does not provide `X-Trace-Id`. The same header is propagated to the Account Service and returned in responses.

Both services expose `GET /metrics`, which returns request and error counts by route.

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

New events return `201 Created`. Duplicate `eventId` submissions return `200 OK` with the original event and `"duplicate": true`; the account balance is not changed again.
