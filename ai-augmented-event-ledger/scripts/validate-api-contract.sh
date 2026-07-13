#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_files=(
  "API_CONTRACT.md"
  "event-gateway/src/main/java/com/schwab/eventledger/gateway/EventController.java"
  "account-service/src/main/java/com/schwab/eventledger/account/AccountController.java"
  "common/src/main/java/com/schwab/eventledger/common/TransactionEventRequest.java"
)

for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || {
    echo "Missing required file: $file" >&2
    exit 1
  }
done

checks=(
  "POST /events"
  "GET /events/{eventId}"
  "GET /events/{eventId}/audit"
  "GET /events?account={accountId}"
  "GET /accounts/{accountId}/balance"
  "GET /accounts/{accountId}"
  "X-Trace-Id"
  "EVENT_ACCEPTED"
  "DUPLICATE_SUBMISSION"
  "EVENT_QUEUED_FOR_RETRY"
)

for check in "${checks[@]}"; do
  if ! grep -Fq "$check" API_CONTRACT.md; then
    echo "API contract missing expected reference: $check" >&2
    exit 1
  fi
done

controller_checks=(
  "@PostMapping(\"/events\")"
  "@GetMapping(\"/events/{eventId}\")"
  "@GetMapping(\"/events/{eventId}/audit\")"
  "@GetMapping(\"/events\")"
  "@GetMapping(\"/accounts/{accountId}/balance\")"
  "@GetMapping(\"/accounts/{accountId}\")"
)

for check in "${controller_checks[@]}"; do
  if ! grep -Fq "$check" event-gateway/src/main/java/com/schwab/eventledger/gateway/EventController.java; then
    echo "Gateway controller missing expected mapping: $check" >&2
    exit 1
  fi
done

echo "API contract validation passed"
