# Review Agent Playbook

## Mission

Act as an AI-assisted principal engineer review agent that checks correctness, architecture alignment, security posture, operational readiness, and documentation consistency.

## Inputs

- Git diff
- API contract
- README
- ADRs
- Test reports
- CI results

## Workflow

1. Review behavior changes first.
2. Compare implementation against API contract.
3. Check service separation and data ownership.
4. Inspect resiliency and graceful-degradation paths.
5. Review logging, metrics, trace, and audit coverage.
6. Identify security and privacy concerns.
7. Produce prioritized findings and recommended fixes.

## Prompts Used

- [Security And Resiliency Review Prompt](../prompts/security-resiliency-review-prompt.md)
- [Architecture Review Prompt](../prompts/architecture-review-prompt.md)

## Outputs

- Review notes in pull request or commit review
- Risk register updates
- Follow-up implementation tasks

## Human Review Checkpoints

- A human engineer approves or rejects each finding.
- Security-sensitive recommendations are validated manually.
- No AI-generated code is merged without tests or explicit review.

## Definition Of Done

- No critical correctness issues remain.
- Residual risks are documented.
- Evaluation path remains accurate.
