# Event Ledger API Contract

This project has two independently runnable Spring Boot services. They communicate only through HTTP/JSON using the DTOs in the `common` module. The services do not share a database, repository, Spring application context, cache, or in-process state.

## Runtime Boundary

| Service | Process | Default Port | Database |
|---|---:|---:|---|
| Event Gateway API | `event-gateway` Spring Boot app | `8080` | `jdbc:h2:mem:gatewaydb` |
| Account Service | `account-service` Spring Boot app | `8081` | `jdbc:h2:mem:accountdb` |

In Docker Compose, only the Gateway is published to the host. The Account Service is reachable to the Gateway at `http://account-service:8081` on the Compose network.

The React operations console is published at `http://localhost:3000` in Docker Compose. It calls the Gateway through an nginx `/api` proxy, so the Account Service remains internal.

## Shared Contract Types

The `common` module is intentionally limited to transport contracts:

- Request and response records
- `EventType`
- `X-Trace-Id` header constant

It must not contain persistence code, repositories, service logic, Spring controllers, or mutable shared state.

## Public Gateway API

### `POST /events`

Accepts a client transaction event.

Request:

```json
{
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
}
```

Responses:

- `201 Created` for a newly accepted event
- `202 Accepted` when the event is stored locally and queued for retry because Account Service is unavailable
- `200 OK` with `"duplicate": true` for a duplicate `eventId`
- `400 Bad Request` for validation errors

### `GET /events/{eventId}`

Returns the stored Gateway event record.

### `GET /events/{eventId}/audit`

Returns Gateway-owned lifecycle audit entries for an event. The audit trail is
useful for support, compliance review, debugging duplicate delivery, and tracing
downstream degradation decisions.

Response:

```json
[
  {
    "id": 1,
    "eventId": "evt-001",
    "accountId": "acct-123",
    "action": "EVENT_ACCEPTED",
    "traceId": "demo-trace-001",
    "detail": "Event persisted by Gateway after validation and idempotency claim",
    "createdAt": "2026-05-15T14:02:12Z"
  },
  {
    "id": 2,
    "eventId": "evt-001",
    "accountId": "acct-123",
    "action": "ACCOUNT_APPLY_SUCCEEDED",
    "traceId": "demo-trace-001",
    "detail": "Transaction applied successfully by Account Service",
    "createdAt": "2026-05-15T14:02:12Z"
  }
]
```

Audit actions:

- `EVENT_ACCEPTED`
- `ACCOUNT_APPLY_SUCCEEDED`
- `DUPLICATE_SUBMISSION`
- `EVENT_QUEUED_FOR_RETRY`

### `GET /events?account={accountId}`

Returns Gateway event records for an account ordered by `eventTimestamp`, then `eventId`.

### `GET /accounts/{accountId}/balance`

Gateway proxy endpoint. Calls Account Service `GET /accounts/{accountId}/balance`.

### `GET /accounts/{accountId}`

Gateway proxy endpoint. Calls Account Service `GET /accounts/{accountId}`.

## Internal Account Service API

### `POST /accounts/{accountId}/transactions`

Called only by the Gateway to apply a transaction.

Request:

```json
{
  "eventId": "evt-001",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z"
}
```

Responses:

- `201 Created` when the transaction is applied for the first time
- `200 OK` when the `eventId` already exists and no balance change is made
- `400 Bad Request` for validation errors

### `GET /accounts/{accountId}/balance`

Returns:

```json
{
  "accountId": "acct-123",
  "balance": 150.00,
  "currency": "USD"
}
```

### `GET /accounts/{accountId}`

Returns account balance plus recent transactions.

## Observability Endpoints

Both services expose `GET /health`.

Gateway health diagnostics include:

- `database`
- `eventRows`
- `auditRows`
- `pendingAccountEvents`
- `accountServiceCircuitOpen`

Account Service health diagnostics include:

- `database`
- `transactionRows`

Both services expose `GET /metrics`, returning request and error counts keyed by HTTP method plus endpoint template. The Gateway also exposes `GET /metrics/prometheus`, returning the same Gateway request and error counts in Prometheus text format.

## Cross-Service Headers

The Gateway forwards `X-Trace-Id` to the Account Service. If a client does not provide one, the Gateway creates one for the request.

Both services include the active trace ID in every structured JSON log line as `traceId`, so one client request can be followed across Gateway logs, Gateway-to-Account call logs, and Account Service logs.
