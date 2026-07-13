# ADR 0002: Idempotency Strategy

## Status

Accepted

## Context

Upstream systems may deliver the same event more than once. Duplicate delivery must not create duplicate ledger rows or alter account balance more than once.

## Decision

The Gateway treats `eventId` as the public idempotency key and stores events with a database-level primary key. Duplicate submissions return the originally stored event with `"duplicate": true`.

The Account Service also stores transactions by `eventId`, making transaction application idempotent across Gateway retries.

## Consequences

- Duplicate submissions are safe at both service boundaries.
- Retry behavior can be at-least-once without corrupting account balances.
- A duplicate payload with changed fields does not overwrite the original event.
- The audit trail records duplicate submission decisions for support and traceability.
