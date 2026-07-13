# Release Agent Playbook

## Mission

Prepare a repeatable release candidate by running quality gates, collecting evidence, and producing an evaluation-ready handoff.

## Inputs

- Source code
- CI workflow results
- Test and coverage reports
- Docker Compose configuration
- README and API contract

## Workflow

1. Run `scripts/run-quality-gates.sh`.
2. Generate or refresh coverage summary.
3. Validate API contract references.
4. Validate Docker Compose configuration.
5. Package release notes and the evaluation path.
6. Confirm the repository is clean before final handoff.

## Prompts Used

- [README Documentation Prompt](../prompts/readme-documentation-prompt.md)
- [API Contract Review Prompt](../prompts/api-contract-review-prompt.md)

## Outputs

- [Evaluation Path](../EVALUATION_PATH.md)
- [Quality Gate Report Example](../docs/generated/QUALITY_GATE_REPORT.md)
- CI artifacts: coverage reports and frontend build output

## Human Review Checkpoints

- Confirm commands run from a fresh clone.
- Confirm Docker Compose starts the Gateway, Account Service, and UI.
- Confirm generated reports match the latest commit.

## Definition Of Done

- Quality gates pass.
- Release instructions are clear.
- The system can be evaluated without guessing the intended path.
