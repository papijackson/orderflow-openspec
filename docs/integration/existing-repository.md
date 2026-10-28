# Integrating the Kit Into an Existing Repository

This kit is designed as an overlay, not as a replacement for an existing application.

## 1. Create the Feature Branch

```bash
./scripts/sdd/create-feature-branch.sh add-order-search
```

## 2. Initialize Feature Artifacts

```bash
./scripts/sdd/init-feature.sh specs/order-search
```

## 3. Specification

Complete:
`specs/order-search/spec.md`

Run:

```bash
./scripts/sdd/validate-spec.sh specs/order-search
```

## 4. Plan

Complete `plan.md` only after specification approval.

## 5. Tasks

Complete `tasks.md` and preserve traceability.

Then validate the completed feature artifacts:

```bash
./scripts/sdd/validate-feature.sh specs/order-search
./scripts/sdd/validate-traceability.sh specs/order-search
```

## 6. Implementation

Implement in dependency order. The kit does not replace your application's existing Maven configuration.

## 7. Quality Controls

Integrate:
- ArchUnit;
- Flyway;
- Testcontainers;
- SonarQube;
- dependency/security scanning;
- GitHub Actions.

Use the reference files under `quality/`, `config/`, and `.github/workflows/` as starting points.

## 8. Existing GitHub Actions

Do not blindly replace an existing pipeline. Merge the gates into the current build/deploy workflow.

The desired relationship is:

`SDD gates -> existing build/test/quality gates -> deployment gates`

## 9. Deployment

The production workflow supplied here is a gate/reference only. Connect it to the organization's existing ECS deployment mechanism after human approval and rollback controls are established.
