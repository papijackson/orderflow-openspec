#!/usr/bin/env bash
set -euo pipefail

FAIL=0

for file in .github/instructions/*.instructions.md; do
  if [[ "$(sed -n '1p' "$file")" != "---" ]] ||
     [[ "$(sed -n '2p' "$file")" != applyTo:* ]] ||
     [[ "$(sed -n '3p' "$file")" != "---" ]]; then
    echo "ERROR: invalid applyTo front matter: $file"
    FAIL=1
  fi
done

if grep -q "spring-batch.instructions.md" .github/copilot-instructions.md &&
   [[ ! -f .github/instructions/spring-batch.instructions.md ]]; then
  echo "ERROR: missing referenced Spring Batch instruction"
  FAIL=1
fi

if [[ "$FAIL" -ne 0 ]]; then
  exit 1
fi

echo "Instruction validation passed."
