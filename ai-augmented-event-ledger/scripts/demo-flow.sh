#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
EVENT_ID="${EVENT_ID:-demo-evt-$(date +%Y%m%d%H%M%S)}"
DEBIT_EVENT_ID="${DEBIT_EVENT_ID:-${EVENT_ID}-debit}"
ACCOUNT_ID="${ACCOUNT_ID:-demo-acct-001}"
TRACE_ID="${TRACE_ID:-demo-trace-001}"

section() {
  printf '\n==> %s\n' "$1"
}

request() {
  local method="$1"
  local url="$2"
  local body="${3:-}"

  if [[ -n "$body" ]]; then
    curl -sS -i -X "$method" "$url" \
      -H 'Content-Type: application/json' \
      -H "X-Trace-Id: ${TRACE_ID}" \
      -d "$body"
  else
    curl -sS -i -X "$method" "$url" \
      -H "X-Trace-Id: ${TRACE_ID}"
  fi
}

credit_payload() {
  cat <<JSON
{
    "eventId": "${EVENT_ID}",
    "accountId": "${ACCOUNT_ID}",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T14:02:11Z",
    "metadata": {
      "source": "demo-flow",
      "scenario": "credit",
      "traceId": "${TRACE_ID}"
    }
}
JSON
}

debit_payload() {
  cat <<JSON
{
    "eventId": "${DEBIT_EVENT_ID}",
    "accountId": "${ACCOUNT_ID}",
    "type": "DEBIT",
    "amount": 25.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T13:02:11Z",
    "metadata": {
      "source": "demo-flow",
      "scenario": "out-of-order-debit",
      "traceId": "${TRACE_ID}"
    }
}
JSON
}

section "Scenario"
printf 'Gateway URL: %s\n' "$GATEWAY_URL"
printf 'Account ID : %s\n' "$ACCOUNT_ID"
printf 'Credit ID  : %s\n' "$EVENT_ID"
printf 'Debit ID   : %s\n' "$DEBIT_EVENT_ID"
printf 'Trace ID   : %s\n' "$TRACE_ID"

section "1. Submit CREDIT event"
request POST "${GATEWAY_URL}/events" "$(credit_payload)"

section "2. Submit duplicate CREDIT event"
request POST "${GATEWAY_URL}/events" "$(credit_payload)"

section "3. Submit out-of-order DEBIT event"
request POST "${GATEWAY_URL}/events" "$(debit_payload)"

section "4. Show chronological events"
request GET "${GATEWAY_URL}/events?account=${ACCOUNT_ID}"

section "5. Show CREDIT audit trail"
request GET "${GATEWAY_URL}/events/${EVENT_ID}/audit"

section "6. Show DEBIT audit trail"
request GET "${GATEWAY_URL}/events/${DEBIT_EVENT_ID}/audit"

section "7. Show balance"
request GET "${GATEWAY_URL}/accounts/${ACCOUNT_ID}/balance"

section "8. Show account detail"
request GET "${GATEWAY_URL}/accounts/${ACCOUNT_ID}"

section "9. Show health"
request GET "${GATEWAY_URL}/health"

section "10. Show metrics"
request GET "${GATEWAY_URL}/metrics"

section "11. Show trace path from Docker logs when available"
if command -v docker >/dev/null 2>&1 && docker compose ps >/dev/null 2>&1; then
  docker compose logs --tail=200 event-gateway account-service | grep "$TRACE_ID" || {
    printf 'Trace ID %s was not found in the last 200 Docker log lines.\n' "$TRACE_ID"
  }
else
  printf 'Docker Compose is not available from this shell. Use this command when the stack is running:\n'
  printf 'docker compose logs --tail=200 event-gateway account-service | grep %s\n' "$TRACE_ID"
fi

printf '\n'
