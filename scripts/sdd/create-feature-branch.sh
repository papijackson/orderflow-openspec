#!/usr/bin/env bash
set -euo pipefail

FEATURE="${1:-}"
BASE_BRANCH="${2:-main}"

if [[ -z "$FEATURE" ]]; then
  echo "Usage: $0 <feature-name> [base-branch]"
  exit 2
fi

SAFE_NAME="$(printf '%s' "$FEATURE" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9._-]/-/g' | sed 's/--*/-/g')"
BRANCH="feature/${SAFE_NAME}"

git fetch origin "$BASE_BRANCH"
git switch "$BASE_BRANCH"
git pull --ff-only origin "$BASE_BRANCH"
git switch -c "$BRANCH"

echo "Created branch: $BRANCH"
