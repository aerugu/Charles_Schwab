# Demo Script

## Five-Minute Walkthrough

1. Start the stack.

```bash
docker compose up --build
```

2. Open the React operations console.

```text
http://localhost:3000
```

3. Run the scripted demo.

```bash
./scripts/demo-flow.sh
```

4. Point out the visible outcomes:

- CREDIT submission is accepted.
- Duplicate submission returns the same event without changing balance twice.
- Out-of-order DEBIT appears in chronological event order.
- Audit trail shows event lifecycle entries.
- Balance reflects `CREDIT - DEBIT`.
- Metrics and health endpoints respond.
- Trace ID is propagated across the Gateway and Account Service call.

## Ten-Minute Technical Deep Dive

1. Open `docs/REQUIREMENTS_TRACEABILITY_MATRIX.md`.
2. Explain Gateway-owned event identity and Account-owned transaction state.
3. Walk through `docs/adr/0002-idempotency-strategy.md`.
4. Walk through `docs/adr/0003-resiliency-patterns.md`.
5. Run `./scripts/run-quality-gates.sh`.
6. Open `docs/AI_ENGINEERING_SCORECARD.md`.
7. Open `docs/agent-runs` to show the repeatable AI-assisted SDLC flow.

## Failure-Mode Demo

1. Start only the Gateway or stop the Account Service container.
2. Submit an event through `POST /events`.
3. Observe `202 Accepted` when the Gateway queues the event locally.
4. Confirm `GET /events/{eventId}` and `GET /events?account=...` still work.
5. Confirm balance queries return a clear unavailable response.
6. Restart the Account Service and allow the retry worker to replay the event.

## AI-Assisted SDLC Evidence Path

Use this sequence to explain the engineering workflow:

1. `agents/design-agent.md`
2. `prompts/architecture-review-prompt.md`
3. `docs/adr`
4. `agents/development-agent.md`
5. `agents/qa-agent.md`
6. `docs/agent-runs`
7. `docs/REQUIREMENTS_TRACEABILITY_MATRIX.md`
8. `docs/AI_ENGINEERING_SCORECARD.md`
9. `.github/workflows/ai-augmented-event-ledger-ci.yml`
