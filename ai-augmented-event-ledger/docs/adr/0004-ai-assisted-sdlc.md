# ADR 0004: AI-Assisted SDLC

## Status

Accepted

## Context

The evaluation emphasizes AI-augmented software engineering practices. The system should show how AI tools, coding assistants, custom agents, and automation can improve productivity, quality, and delivery outcomes without making financial runtime behavior nondeterministic.

## Decision

The application represents AI assistance through repeatable SDLC artifacts:

- Agent playbooks for design, development, QA, review, and release
- Reusable prompt templates
- AI-assisted traceability documentation
- Quality-gate automation scripts
- CI workflow
- Generated-output examples

The runtime application does not call LLMs during transaction processing.

## Consequences

- The AI-assisted workflow can be inspected directly.
- Engineering controls remain explicit: human review, tests, contract validation, and quality gates.
- The application avoids overclaiming runtime AI behavior.
- The approach is reusable for future feature work.
