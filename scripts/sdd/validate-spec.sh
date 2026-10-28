#!/usr/bin/env bash
set -euo pipefail

FEATURE="${1:-}"
if [[ -z "$FEATURE" ]]; then
  echo "Usage: $0 <feature-directory>"
  exit 2
fi

SPEC="$FEATURE/spec.md"
if [[ ! -s "$SPEC" ]]; then
  echo "ERROR: missing or empty $SPEC"
  exit 1
fi

FAIL=0
for marker in \
  "Functional Requirements" \
  "Acceptance Criteria" \
  "Database Migration" \
  "Security Requirements" \
  "Observability Requirements" \
  "Performance Requirements" \
  "Architecture Constraints"; do
  if ! grep -q "$marker" "$SPEC"; then
    echo "ERROR: spec.md missing section: $marker"
    FAIL=1
  fi
done

if [[ "$FAIL" -ne 0 ]]; then
  exit 1
fi

echo "Specification validation passed."
