# ADR 0003: Resiliency Patterns

## Status

Accepted

## Context

The Account Service may be slow or unavailable. The Gateway must not hang, retry indefinitely, or leak internal failures to clients as uncontrolled `500` errors.

## Decision

The Gateway protects Account Service calls with:

- Connection/read timeout
- Bounded retry
- Exponential backoff
- Jitter
- Circuit breaker
- Pending outbox fallback for submitted events

Balance and account-detail proxy calls return controlled `503 Service Unavailable` responses when the downstream dependency is unreachable.

## Consequences

- Slow or failing downstream calls are bounded.
- Repeated failures open the circuit and fail fast.
- Submitted events can be accepted and queued for retry when Account Service is down.
- Operational state is visible through logs, metrics, health diagnostics, and audit entries.
