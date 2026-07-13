#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

report_csv() {
  local label="$1"
  local file="$2"
  if [[ ! -f "$file" ]]; then
    printf '| %s | not generated | - |\n' "$label"
    return
  fi

  awk -F',' -v label="$label" '
    NR > 1 {
      missed += $4;
      covered += $5;
    }
    END {
      total = missed + covered;
      pct = total == 0 ? 0 : (covered / total) * 100;
      printf "| %s | %.2f%% | %s/%s instructions covered |\n", label, pct, covered, total;
    }
  ' "$file"
}

printf '# Coverage Summary\n\n'
printf '| Report | Instruction Coverage | Detail |\n'
printf '|---|---:|---|\n'
report_csv "Account Service Unit" "account-service/target/site/jacoco-unit/jacoco.csv"
report_csv "Gateway Unit" "event-gateway/target/site/jacoco-unit/jacoco.csv"
report_csv "Account Service Functional" "account-service/target/site/jacoco-functional/jacoco.csv"
report_csv "Gateway Functional" "event-gateway/target/site/jacoco-functional/jacoco.csv"
