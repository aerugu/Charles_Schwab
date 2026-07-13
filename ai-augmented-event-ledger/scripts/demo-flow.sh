#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
EVENT_ID="${EVENT_ID:-demo-evt-001}"
ACCOUNT_ID="${ACCOUNT_ID:-demo-acct-001}"
TRACE_ID="${TRACE_ID:-demo-trace-001}"

echo "Submitting demo event to ${GATEWAY_URL}"
curl -sS -i -X POST "${GATEWAY_URL}/events" \
  -H 'Content-Type: application/json' \
  -H "X-Trace-Id: ${TRACE_ID}" \
  -d "{
    \"eventId\": \"${EVENT_ID}\",
    \"accountId\": \"${ACCOUNT_ID}\",
    \"type\": \"CREDIT\",
    \"amount\": 150.00,
    \"currency\": \"USD\",
    \"eventTimestamp\": \"2026-05-15T14:02:11Z\",
    \"metadata\": {
      \"source\": \"demo-flow\",
      \"agent\": \"release-agent\"
    }
  }"

printf '\n\nFetching chronological events\n'
curl -sS "${GATEWAY_URL}/events?account=${ACCOUNT_ID}"

printf '\n\nFetching audit trail\n'
curl -sS "${GATEWAY_URL}/events/${EVENT_ID}/audit"

printf '\n\nFetching balance\n'
curl -sS "${GATEWAY_URL}/accounts/${ACCOUNT_ID}/balance"

printf '\n\nFetching health\n'
curl -sS "${GATEWAY_URL}/health"

printf '\n'
