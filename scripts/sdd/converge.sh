#!/usr/bin/env bash
set -euo pipefail

FEATURE="${1:-}"

echo "=== SDD convergence ==="

if [[ ! -f pom.xml ]]; then
  echo "ERROR: pom.xml not found. This kit expects a Maven Java project."
  exit 1
fi

echo "--- Maven verify ---"
./mvnw -B verify 2>/dev/null || mvn -B verify

echo "--- Architecture evidence ---"
if grep -Rqs "com.tngtech.archunit" pom.xml; then
  echo "ArchUnit dependency detected; Maven verify executes its tests when configured."
else
  echo "WARNING: ArchUnit dependency not detected. Add architecture tests before production readiness."
fi

echo "--- Flyway evidence ---"
if grep -Rqs "flyway" pom.xml; then
  echo "Flyway dependency detected."
  if [[ -d src/main/resources/db/migration ]]; then
    ./scripts/sdd/validate-flyway.sh
  else
    echo "WARNING: Flyway is configured but no migration directory was found."
  fi
else
  echo "WARNING: Flyway dependency not detected. If persistence is used, Flyway is mandatory."
fi

echo "--- Metric-tag heuristic ---"
if ! ./scripts/sdd/check-metric-tags.sh; then
  echo "WARNING: review potential metric-tag findings before production readiness."
fi

if [[ -n "$FEATURE" ]]; then
  echo "--- Feature artifacts ---"
  ./scripts/sdd/validate-feature.sh "$FEATURE"
  ./scripts/sdd/validate-traceability.sh "$FEATURE"
else
  echo "WARNING: no feature directory supplied; feature and traceability validation skipped."
fi

echo "--- Manual evidence required ---"
echo "Security scanning, observability review, and performance evidence remain"
echo "project-specific gates. Record their evidence in the feature artifacts."
echo "--- Automated convergence checks complete ---"
