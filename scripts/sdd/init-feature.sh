#!/usr/bin/env bash
set -euo pipefail

FEATURE="${1:-}"
if [[ -z "$FEATURE" ]]; then
  echo "Usage: $0 <feature-directory>"
  exit 2
fi

mkdir -p "$FEATURE"
cp .specify/templates/spec-template.md "$FEATURE/spec.md"
cp .specify/templates/plan-template.md "$FEATURE/plan.md"
cp .specify/templates/tasks-template.md "$FEATURE/tasks.md"

echo "Created:"
echo "  $FEATURE/spec.md"
echo "  $FEATURE/plan.md"
echo "  $FEATURE/tasks.md"
echo
echo "Next: complete spec.md and run scripts/sdd/validate-spec.sh $FEATURE"
