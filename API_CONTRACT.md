# Event Ledger API Contract

This project has two independently runnable Spring Boot services. They communicate only through HTTP/JSON using the DTOs in the `common` module. The services do not share a database, repository, Spring application context, cache, or in-process state.

## Runtime Boundary

| Service | Process | Default Port | Database |
|---|---:|---:|---|
| Event Gateway API | `event-gateway` Spring Boot app | `8080` | `jdbc:h2:mem:gatewaydb` |
| Account Service | `account-service` Spring Boot app | `8081` | `jdbc:h2:mem:accountdb` |

In Docker Compose, only the Gateway is published to the host. The Account Service is reachable to the Gateway at `http://account-service:8081` on the Compose network.

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
- `200 OK` with `"duplicate": true` for a duplicate `eventId`
- `400 Bad Request` for validation errors
- `503 Service Unavailable` when the Account Service cannot apply the transaction

### `GET /events/{eventId}`

Returns the stored Gateway event record.

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

## Cross-Service Headers

The Gateway forwards `X-Trace-Id` to the Account Service. If a client does not provide one, the Gateway creates one for the request.
