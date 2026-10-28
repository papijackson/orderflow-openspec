#!/usr/bin/env bash
set -euo pipefail

FAIL=0

required=(
  ".specify/memory/constitution.md"
  ".github/copilot-instructions.md"
  ".github/instructions/architecture.instructions.md"
  ".github/instructions/spring-batch.instructions.md"
  ".github/instructions/testing.instructions.md"
  ".github/instructions/jpa-postgresql.instructions.md"
  ".specify/sdd-workflow/workflow.md"
)

for f in "${required[@]}"; do
  if [[ -f "$f" ]]; then
    echo "OK  $f"
  else
    echo "FAIL $f"
    FAIL=1
  fi
done

if [[ ! -f sdd-kit.yml ]]; then
  echo "FAIL sdd-kit.yml"
  FAIL=1
fi

if ! ./scripts/sdd/validate-instructions.sh; then
  FAIL=1
fi

if [[ "$FAIL" -ne 0 ]]; then
  echo "Repository kit validation failed."
  exit 1
fi

echo "Repository kit validation passed."
