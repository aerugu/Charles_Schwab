# Quality Gate Report

This report captures the reviewer-oriented quality gates encoded by `scripts/run-quality-gates.sh`.

| Gate | Command | Local Result |
|---|---|---|
| API contract validation | `scripts/validate-api-contract.sh` | Pass |
| Backend unit tests | `mvn test` | Pass |
| Backend functional tests and coverage | `mvn clean verify` | Pass |
| Docker Compose config | `docker compose config --quiet` | Pass |
| Coverage summary | `scripts/generate-coverage-summary.sh` | Pass |
| Frontend build | `npm install && npm run build` | Covered in CI; local script skips when npm is unavailable |

## Latest Coverage Summary

| Report | Instruction Coverage | Detail |
|---|---:|---|
| Account Service Unit | 26.56% | 149/561 instructions covered |
| Gateway Unit | 41.92% | 967/2307 instructions covered |
| Account Service Functional | 84.14% | 472/561 instructions covered |
| Gateway Functional | 72.39% | 1670/2307 instructions covered |

## Notes

- Functional tests start embedded Spring Boot servers on random local ports.
- In restricted sandboxes, integration tests may need permission to bind local ports.
- Coverage HTML files are generated under each service's `target/site` directory.
- The CI workflow builds the React operations console on GitHub-hosted runners with Node available.
