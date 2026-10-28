#!/usr/bin/env bash
set -euo pipefail

FEATURE="${1:-}"
if [[ -z "$FEATURE" ]]; then
  echo "Usage: $0 <feature-directory>"
  exit 2
fi

FAIL=0

./scripts/sdd/validate-spec.sh "$FEATURE"

for f in plan.md tasks.md; do
  if [[ ! -s "$FEATURE/$f" ]]; then
    echo "ERROR: missing or empty $FEATURE/$f"
    FAIL=1
  else
    echo "OK: $FEATURE/$f"
  fi
done

if [[ "$FAIL" -ne 0 ]]; then
  exit 1
fi

echo "Feature structure validation passed."
