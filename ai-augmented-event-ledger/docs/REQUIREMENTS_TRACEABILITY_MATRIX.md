# Requirements Traceability Matrix

This matrix maps functional, distributed-systems, observability, resiliency, and AI-assisted SDLC requirements to implementation evidence, tests, automation, and documentation.

| Requirement | Implementation Evidence | Test Evidence | Automation Check | Documentation Evidence |
|---|---|---|---|---|
| Event submission through `POST /events` | `event-gateway/src/main/java/com/schwab/eventledger/gateway/EventController.java`, `EventService.java` | `GatewayIntegrationTest` | `mvn clean verify` | `API_CONTRACT.md`, `README.md` |
| Idempotency by `eventId` | `EventService.java`, `EventRepository.java`, `TransactionRepository.java` | `GatewayIntegrationTest`, `GatewayAccountEndToEndIntegrationTest`, `TransactionRepositoryTest` | `mvn clean verify` | `docs/adr/0002-idempotency-strategy.md` |
| Duplicate submission returns original event | `EventService.java`, `EventResponse.java` | `GatewayIntegrationTest`, `GatewayConcurrentFailureIntegrationTest` | `mvn clean verify` | `API_CONTRACT.md` |
| Out-of-order event tolerance | `EventRepository.java` query ordering by `event_timestamp` | `GatewayIntegrationTest` | `mvn clean verify`, `scripts/demo-flow.sh` | `docs/DESIGN_AGENT.md` |
| Balance equals credits minus debits | `AccountService.java`, `TransactionRepository.java` | `AccountServiceIntegrationTest`, `GatewayAccountEndToEndIntegrationTest` | `mvn clean verify` | `API_CONTRACT.md` |
| Validation rejects missing or invalid input | `GlobalExceptionHandler.java`, request validation annotations in common DTOs | `GatewayIntegrationTest`, `GatewayGracefulDegradationTest` | `mvn clean verify` | `API_CONTRACT.md` |
| Event Gateway and Account Service are independent processes | `Dockerfile.gateway`, `Dockerfile.account`, `docker-compose.yml` | `AccountServiceIntegrationTest`, `GatewayAccountEndToEndIntegrationTest` | `docker compose config --quiet` | `docs/adr/0001-service-separation.md` |
| Each service owns its own embedded database | Account and Gateway Spring datasource config, schema initializers | `usesDedicatedAccountServiceDatabase`, Gateway integration tests | `mvn clean verify` | `docs/adr/0001-service-separation.md` |
| Synchronous REST between services | `AccountClient.java`, `AccountController.java` | `GatewayAccountEndToEndIntegrationTest`, `AccountServiceContractTest` | `mvn clean verify` | `API_CONTRACT.md` |
| Trace ID generation and propagation | `TraceFilter.java`, `AccountClient.java`, `TraceHeaders.java` | `GatewayAccountEndToEndIntegrationTest`, `GatewayIntegrationTest` | `mvn clean verify`, `scripts/demo-flow.sh` | `AI_ASSISTED_SDLC.md` |
| Structured JSON logs | `JsonLogger.java` in both services | `JsonLoggerTest` in both services | `mvn test` | `README.md` |
| Health endpoints | `EventController.java`, `AccountController.java` | `GatewayIntegrationTest`, `AccountServiceIntegrationTest` | `scripts/demo-flow.sh` | `API_CONTRACT.md` |
| Metrics endpoints | `MetricsFilter.java`, `MetricsRegistry.java`, controllers | `GatewayIntegrationTest`, `AccountServiceIntegrationTest` | `scripts/demo-flow.sh` | `README.md` |
| Timeout, retry, backoff, jitter, circuit breaker | `AccountClient.java`, `SimpleCircuitBreaker.java`, resiliency properties | `SimpleCircuitBreakerTest`, `GatewayResiliencyIntegrationTest` | `mvn clean verify` | `docs/adr/0003-resiliency-patterns.md` |
| Graceful degradation when Account Service is down | `EventService.java`, pending event repository methods, proxy error handling | `GatewayGracefulDegradationTest`, `GatewayResiliencyIntegrationTest` | `mvn clean verify` | `API_CONTRACT.md`, `docs/adr/0003-resiliency-patterns.md` |
| Async fallback and replay | `PendingEventRetryWorker.java`, pending event repository methods | `GatewayResiliencyIntegrationTest` | `mvn clean verify` | `docs/DESIGN_AGENT.md` |
| Gateway audit trail | `AuditRepository.java`, `AuditEntry.java`, `GET /events/{eventId}/audit` | `GatewayIntegrationTest` | `scripts/demo-flow.sh` | `API_CONTRACT.md` |
| React operations console | `frontend/src/App.tsx`, `frontend/src/styles.css` | CI frontend build | GitHub Actions workflow | `README.md` |
| API contract validation | `scripts/validate-api-contract.sh` | Script execution | `scripts/run-quality-gates.sh` | `API_CONTRACT.md` |
| Unit and functional coverage reports | Maven Surefire, Failsafe, JaCoCo config | Maven test reports | `scripts/generate-coverage-summary.sh` | `docs/generated/QUALITY_GATE_REPORT.md` |
| AI-assisted SDLC evidence | Agent playbooks, prompt templates, agent-run logs | Traceability docs and quality gates | `scripts/run-quality-gates.sh` | `AI_ASSISTED_SDLC.md`, `docs/agent-runs` |
