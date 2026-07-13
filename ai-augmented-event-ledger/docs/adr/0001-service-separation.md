# ADR 0001: Service Separation

## Status

Accepted

## Context

The system receives financial transaction events through a public Event Gateway and applies them to account state through an internal Account Service. The assignment requires independently runnable services with separate embedded databases.

## Decision

The Event Gateway and Account Service are separate Spring Boot applications. Each service owns its own H2 database, schema initialization, repositories, controllers, logs, health checks, and metrics.

The `common` module contains only transport DTOs and shared constants. It does not contain repositories, service logic, or mutable shared state.

## Consequences

- Service ownership is explicit and reviewable.
- The Gateway can continue serving event reads during Account Service outages.
- The Account Service remains the owner of balance and transaction history.
- Cross-service behavior must be validated through REST integration tests.
