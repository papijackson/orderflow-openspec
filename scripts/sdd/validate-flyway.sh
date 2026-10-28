#!/usr/bin/env bash
set -euo pipefail

if [[ ! -d src/main/resources/db/migration ]]; then
  echo "No Flyway migration directory found."
  echo "If this application uses PostgreSQL/JPA, create src/main/resources/db/migration."
  exit 1
fi

FAIL=0
while IFS= read -r file; do
  name="$(basename "$file")"
  if [[ ! "$name" =~ ^V[0-9]{8}_[0-9]{3}__.+\.sql$ ]]; then
    echo "ERROR: migration naming does not follow baseline: $name"
    FAIL=1
  fi
done < <(find src/main/resources/db/migration -type f -name '*.sql' | sort)

if [[ "$FAIL" -ne 0 ]]; then exit 1; fi
echo "Flyway naming validation passed."
