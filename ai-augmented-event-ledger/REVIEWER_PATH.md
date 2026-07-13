# Reviewer Path

## Start Here

1. Read the root purpose in [README.md](README.md).
2. Review the architecture diagram and service summary.
3. Open the AI-assisted SDLC traceability map: [AI_ASSISTED_SDLC.md](AI_ASSISTED_SDLC.md).

## Run This Command

For the full backend verification path:

```bash
./scripts/run-quality-gates.sh
```

For Docker-based runtime review:

```bash
docker compose up --build
```

## See This UI

Open:

```text
http://localhost:3000
```

Use the React operations console to:

- Submit a CREDIT or DEBIT event.
- View chronological events by account.
- Inspect `GET /events/{eventId}/audit`.
- Query account balance and recent transactions.
- Review health and metrics.

## Review These Agent Deliverables

- [Design Agent Playbook](agents/design-agent.md)
- [Development Agent Playbook](agents/development-agent.md)
- [QA Agent Playbook](agents/qa-agent.md)
- [Review Agent Playbook](agents/review-agent.md)
- [Release Agent Playbook](agents/release-agent.md)

## Review These Prompt Templates

- [Architecture Review Prompt](prompts/architecture-review-prompt.md)
- [API Contract Review Prompt](prompts/api-contract-review-prompt.md)
- [Test Generation Prompt](prompts/test-generation-prompt.md)
- [Security And Resiliency Review Prompt](prompts/security-resiliency-review-prompt.md)
- [README Documentation Prompt](prompts/readme-documentation-prompt.md)

## Check These Tests And Coverage Reports

Generate reports:

```bash
mvn clean verify
./scripts/generate-coverage-summary.sh
```

Open reports:

- `account-service/target/site/jacoco-unit/index.html`
- `event-gateway/target/site/jacoco-unit/index.html`
- `account-service/target/site/jacoco-functional/index.html`
- `event-gateway/target/site/jacoco-functional/index.html`

## Inspect Architecture Decisions

- [0001 Service Separation](docs/adr/0001-service-separation.md)
- [0002 Idempotency Strategy](docs/adr/0002-idempotency-strategy.md)
- [0003 Resiliency Patterns](docs/adr/0003-resiliency-patterns.md)
- [0004 AI-Assisted SDLC](docs/adr/0004-ai-assisted-sdlc.md)
