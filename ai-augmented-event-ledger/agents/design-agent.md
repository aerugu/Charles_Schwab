# Design Agent Playbook

## Mission

Accelerate architecture discovery and turn ambiguous requirements into a reviewable technical design that preserves financial-system correctness, service ownership, and operational resilience.

## Inputs

- Assignment requirements and constraints
- Existing `API_CONTRACT.md`
- Current source tree and tests
- Known distributed-systems risks: duplicate delivery, out-of-order arrival, partial outages, traceability, and data ownership

## Workflow

1. Parse requirements into capabilities, constraints, and non-goals.
2. Identify domain boundaries and service ownership.
3. Produce candidate architecture options and tradeoffs.
4. Select the simplest architecture that satisfies the constraints.
5. Generate diagrams and ADR candidates.
6. Hand off implementation-ready decisions to the Development Agent.

## Prompts Used

- [Architecture Review Prompt](../prompts/architecture-review-prompt.md)
- [API Contract Review Prompt](../prompts/api-contract-review-prompt.md)

## Outputs

- [Design Agent Deliverable](../docs/DESIGN_AGENT.md)
- [Service Separation ADR](../docs/adr/0001-service-separation.md)
- [Idempotency ADR](../docs/adr/0002-idempotency-strategy.md)
- [Resiliency ADR](../docs/adr/0003-resiliency-patterns.md)

## Human Review Checkpoints

- Confirm no shared database or in-process state crosses service boundaries.
- Confirm architecture handles duplicate and out-of-order events without relying on arrival order.
- Confirm failure behavior is explicit and client-safe.
- Confirm diagrams match the implementation.

## Definition Of Done

- Architecture is explainable in less than five minutes.
- Tradeoffs are documented.
- Service contracts are clear.
- Risks and production-evolution paths are visible.
