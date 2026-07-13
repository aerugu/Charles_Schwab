#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() {
  printf '\n==> %s\n' "$1"
}

log "Validating API contract"
./scripts/validate-api-contract.sh

log "Running backend unit tests"
mvn test

log "Running backend functional tests and coverage"
mvn clean verify

if command -v npm >/dev/null 2>&1; then
  log "Building React operations console"
  (cd frontend && npm install && npm run build)
else
  log "Skipping React build because npm is not on PATH"
fi

if command -v docker >/dev/null 2>&1; then
  log "Validating Docker Compose configuration"
  docker compose config --quiet
else
  log "Skipping Docker Compose validation because docker is not on PATH"
fi

log "Generating coverage summary"
./scripts/generate-coverage-summary.sh

log "Quality gates completed"
