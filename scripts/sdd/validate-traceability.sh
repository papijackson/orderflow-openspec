#!/usr/bin/env bash
set -euo pipefail

FEATURE="${1:-}"
if [[ -z "$FEATURE" ]]; then
  echo "Usage: $0 <feature-directory>"
  exit 2
fi

FAIL=0

grep -q "Traceability" "$FEATURE/spec.md" || { echo "ERROR: spec traceability missing"; FAIL=1; }
grep -q "Traceability" "$FEATURE/plan.md" || { echo "ERROR: plan traceability missing"; FAIL=1; }
grep -q "Traceability" "$FEATURE/tasks.md" || { echo "ERROR: task traceability missing"; FAIL=1; }

if ! grep -Eq "Spec|Requirement" "$FEATURE/tasks.md"; then
  echo "ERROR: tasks do not appear to reference requirements"
  FAIL=1
fi

if [[ "$FAIL" -ne 0 ]]; then exit 1; fi
echo "Traceability validation passed."
