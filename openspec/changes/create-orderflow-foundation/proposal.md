# Proposal

## Why

OrderFlow has no application source or build foundation yet, so there is no consistent place to add future capabilities or verify their architecture and behavior. Establishing a small Spring Boot baseline now gives subsequent work a tested, environment-configurable application boundary without prematurely implementing order-processing behavior or distributed infrastructure.

## What Changes

- Create a Maven-based Java 21 / Spring Boot 4 application baseline.
- Prepare the documented Spring Data JPA, PostgreSQL, and Flyway baseline without introducing business tables.
- Define and enforce the documented layered package boundaries.
- Provide externalized, environment-specific configuration without committing secrets.
- Establish a consistent, sanitized REST error response contract.
- Add focused automated tests for configuration, REST error mapping, and architecture boundaries.
- Add a GitHub Actions workflow to build and test pull requests and pushes; it will not deploy.

## Capabilities

### New Capabilities
- `application-configuration`: Load and validate application settings from external configuration suitable for separate environments.
- `layered-application-architecture`: Keep application dependencies within the prescribed Controller, Service, Repository, and Entity boundaries.
- `rest-error-contract`: Return consistent, safe REST error responses for framework-level validation and unexpected failures.
- `build-validation`: Run the Maven build, automated tests, and architecture checks in GitHub Actions and fail the check when a required validation fails.

### Modified Capabilities
- None.

## Impact

- **Application/build:** New Maven project and Spring Boot application source; exact Java package root remains to be confirmed.
- **Runtime configuration:** External configuration conventions and environment/test configuration; no production deployment resources.
- **REST:** Shared error mapping and response contract. This proposal assumes Spring `ProblemDetail` (RFC 9457) as the response format; confirm during review before treating it as a stable public contract.
- **Tests:** JUnit Jupiter, Spring Boot test support, ArchUnit, and PostgreSQL Testcontainers for persistence wiring. No business acceptance scenarios are introduced.
- **CI:** A GitHub Actions build-and-test workflow, with no AWS integration or deployment steps.
- **Out of scope:** Order business behavior, real AWS integrations, Spring Batch jobs, production deployment, and distributed microservices.
- **Assumptions to validate:** This repository is the intended home for the application; the package root and environment names are not yet agreed. PostgreSQL/Flyway remain the documented project standard; this change creates no business schema.
