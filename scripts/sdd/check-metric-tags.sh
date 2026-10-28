#!/usr/bin/env bash
set -euo pipefail

# Heuristic guardrail. This is intentionally conservative and should be
# complemented by code review and Micrometer tests.
PATTERN='customerId|orderId|requestId|sessionId|UUID|email'
if grep -RInE "$PATTERN" src/main/java 2>/dev/null | grep -Ei 'tag|meter' ; then
  echo "WARNING: potential high-cardinality metric tag detected."
  echo "Review every match; this script is advisory unless promoted to blocking."
  exit 1
fi

echo "No obvious forbidden metric-tag pattern detected."
