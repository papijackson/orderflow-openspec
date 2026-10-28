# GitHub Actions Integration

The supplied workflows are reference implementations.

## Existing Pipeline

If the repository already has build/deploy workflows, keep them and integrate the SDD gates rather than creating competing pipelines.

Recommended order:

1. specification/branch checks where applicable;
2. compile;
3. unit tests;
4. static analysis;
5. architecture tests;
6. integration/Testcontainers;
7. Flyway validation;
8. Cucumber acceptance;
9. security/dependency scan;
10. SonarQube;
11. performance smoke where required;
12. production readiness;
13. human approval;
14. ECS deployment;
15. post-deployment verification.

## Secrets

Use GitHub environment/repository secrets for:
- Sonar token;
- Sonar host;
- deployment credentials.

Never put secrets in workflow files.

## Branch Protection

Require appropriate checks before merge. Production deployment should use a protected environment requiring human approval.
