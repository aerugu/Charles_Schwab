# QA Agent Deliverable

## Objective

The QA Agent was used to convert requirements into automated checks and repeatable verification commands. The test strategy covers positive paths, negative paths, resiliency behavior, trace propagation, and full Gateway to Account Service integration.

## Test Coverage Areas

| Area | Coverage |
|---|---|
| Core functionality | Idempotency, out-of-order event listing, balance calculation, validation failures. |
| Service separation | Dedicated Gateway and Account Service H2 database assertions. |
| Resiliency | Account Service failure simulation, circuit breaker behavior, graceful `503` responses, pending outbox. |
| Trace propagation | Gateway generated/provided trace IDs are propagated to Account Service. |
| Auditing | Event audit entries are generated for accepted, applied, duplicate, and queued outcomes. |
| Integration | End-to-end Gateway to real Account Service flow. |
| Observability | Health, JSON logging, metrics, and Prometheus-compatible metrics endpoints. |

## Standard Commands

Run unit tests:

```bash
mvn test
```

Run unit tests, functional tests, and generate coverage:

```bash
mvn clean verify
```

## Coverage Reports

The build generates unit and functional JaCoCo reports:

- `account-service/target/site/jacoco-unit/index.html`
- `event-gateway/target/site/jacoco-unit/index.html`
- `account-service/target/site/jacoco-functional/index.html`
- `event-gateway/target/site/jacoco-functional/index.html`

CSV files are generated beside the HTML reports for machine-readable coverage review.

## QA Agent Acceptance Criteria

- All Maven modules compile successfully.
- Unit tests pass with `mvn test`.
- Functional tests pass with `mvn clean verify`.
- Coverage reports are generated for both services.
- Audit endpoint verifies traceable lifecycle decisions for submitted events.
- Service outage scenarios return controlled responses instead of hanging or leaking internal exceptions.
