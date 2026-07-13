# Development Agent Playbook

## Mission

Convert approved design decisions into working, maintainable implementation increments with clear error handling, logging, auditing, and meaningful commits.

## Inputs

- Design Agent outputs
- ADRs
- API contract
- Existing source conventions
- Test failures and review feedback

## Workflow

1. Inspect current implementation before editing.
2. Create narrowly scoped implementation tasks.
3. Implement with constructor injection, immutable DTOs, and service-owned persistence.
4. Add or update error handling and structured logs.
5. Add audit or trace points for important lifecycle decisions.
6. Run targeted tests.
7. Prepare a concise commit summary.

## Prompts Used

- [Security And Resiliency Review Prompt](../prompts/security-resiliency-review-prompt.md)
- [README Documentation Prompt](../prompts/readme-documentation-prompt.md)

## Outputs

- [Development Agent Deliverable](../docs/DEVELOPMENT_AGENT.md)
- Gateway audit trail endpoint: `GET /events/{eventId}/audit`
- React operations console
- Docker Compose integration

## Human Review Checkpoints

- Verify implementation stays within service boundaries.
- Check that failures return controlled HTTP responses.
- Confirm audit entries do not leak sensitive data.
- Confirm UI calls only Gateway APIs and does not expose the Account Service directly.

## Definition Of Done

- Code compiles.
- Tests pass.
- Logs, metrics, and audit entries support diagnosis.
- Commit history is meaningful and scoped.
