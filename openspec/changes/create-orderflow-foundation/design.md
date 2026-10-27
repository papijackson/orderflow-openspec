# Design

## Context

See `proposal.md` for the motivation and scope. The repository currently has no application source, Maven project, or build/test workflow; its only GitHub Actions workflow configures the Copilot environment. OpenSpec is configured with the `spec-driven` schema and has no existing capability specs.

The project guidance sets Java 21, Spring Boot 4, Maven, PostgreSQL, and Flyway as standards. It prescribes a simple `Controller -> Service -> Repository -> Entity` architecture, discourages Clean/Hexagonal/CQRS layers by default, requires externalized secrets, and calls for meaningful unit/integration/acceptance tests and architecture checks in CI.

The requirements for the new capability contracts are in `specs/application-configuration/spec.md`, `specs/layered-application-architecture/spec.md`, `specs/rest-error-contract/spec.md`, and `specs/build-validation/spec.md`.

## Goals / Non-Goals

**Goals:**
- Establish one Maven/Spring Boot application that can evolve without inventing order behavior.
- Make environment-specific settings and REST error behavior explicit and testable.
- Enforce the layer rules in automated tests and run those checks in pull-request/push validation.
- Keep persistence dependencies aligned with the documented stack while introducing no business schema.

**Non-Goals:**
- Define order lifecycle, endpoints, business entities, or business acceptance criteria.
- Add AWS SDK integrations, SNS/SQS messaging, Spring Batch, or distributed services.
- Create production deployment resources, credentials, or environment-specific secret values.
- Add a separate domain model, generic service interfaces, or architectural layers without a demonstrated requirement.

## Decisions

### Single-module layered monolith

Create one Spring Boot application in one Maven module. Use a project-selected base package with layer packages such as `controller`, `service`, `repository`, `entity`, `exception`, and `configuration`; create each package only when it has a real responsibility. Controllers handle HTTP and validation, services own use cases and transaction boundaries, repositories own persistence, and entities remain the default persistence/application model. Use boundary DTOs where an external contract or data-exposure concern warrants them.

This follows the existing repository rules and keeps the foundation proportional to the known requirements. A feature-oriented package layout, Hexagonal/Clean Architecture, CQRS, and microservices were considered but are not justified by the current absence of business boundaries or independent deployment needs.

The exact Java package root is intentionally deferred to the first implementation task; it does not change the required layer boundaries.

### Spring Boot-managed dependencies and configuration

Use the Spring Boot dependency management for Java 21 and Spring Boot 4. Include web and validation support together with the documented Spring Data JPA, PostgreSQL, and Flyway baseline. This change wires persistence configuration and validates startup/migration behavior, but creates no business entities or schema. Use the Actuator/Micrometer Prometheus integration only to the extent needed by the foundation; expose no unnecessary operational endpoints. Do not add AWS or Batch dependencies in this change.

Keep common, non-secret defaults in application configuration and supply deployment-specific values through Spring Boot's external configuration mechanisms, such as environment variables. Use a deterministic test configuration. Use typed configuration binding and startup validation for grouped required settings; do not scatter `@Value` fields through application code or commit credentials. Avoid profile proliferation: introduce profiles only when an actual behavior difference requires one.

The set and naming of deployment environments is not prescribed by the current requirements; implementation must not invent production endpoints or secret values.

### Centralized REST errors use Spring Problem Details

Use Spring Framework's `ProblemDetail` support (RFC 9457) and a centralized `@RestControllerAdvice` for handled validation and application errors. Return `application/problem+json` with an HTTP status consistent with the body. Map request validation failures to 400 and unexpected failures to a generic 500. Keep public detail safe; retain useful diagnostic context only in protected server logs and do not expose stack traces, SQL, credentials, or infrastructure details.

This uses the framework's existing support instead of adding a custom error DTO or dependency. The public format is an assumption explicitly recorded in the proposal/spec; changing it after review requires updating the `rest-error-contract` spec.

### Automated testing is focused on foundation behavior

Use JUnit Jupiter, AssertJ, Mockito, and Spring Boot test support. Use focused web tests for validation/error mapping, configuration tests for valid overrides and invalid required settings, ArchUnit tests that inspect compiled dependencies for forbidden layer crossings, and PostgreSQL Testcontainers to exercise persistence configuration and Flyway startup rather than relying on repository mocks.

Do not add Cucumber scenarios for infrastructure-only setup without business acceptance behavior. Record this as a scoped exception to the usual significant-feature acceptance baseline; add business-readable Cucumber scenarios with the first business capability. The build remains `./mvnw -B verify`, so these test classes and architecture checks run as part of Maven verification.

### GitHub Actions validates, but does not deploy

Add a build-and-test workflow for pull requests and pushes. It checks out the source, configures Temurin Java 21 and Maven caching, then runs the Maven Wrapper with batch verification (`./mvnw -B verify`). A failed build, test, or architecture check must fail the workflow. Do not add secrets, AWS credentials, production deployment, or deployment approvals to this workflow.

This repository has no existing build/deployment workflow to merge with; keep the existing Copilot setup workflow unchanged.

## Risks / Trade-offs

- [Problem Details becomes an externally visible contract] → Keep the format standard and minimal; review the explicit assumption before implementation and change the spec if the API contract should differ.
- [No business use case exists to validate service/repository/entity packages] → Create only packages with real responsibilities and do not add placeholder business classes.
- [PostgreSQL/Testcontainers may make CI dependent on container support] → Use the hosted Linux runner's supported container runtime and keep database integration tests scoped to persistence behavior.
- [Environment names and the package root are undecided] → Select them in the first implementation task; do not embed environment-specific endpoints or credentials in the plan.
- [The foundation may be mistaken for production readiness] → Keep CI build/test-only and state that deployment, runtime IAM, and production configuration are out of scope.

## Migration Plan

This is a greenfield application foundation with no existing application data or deployed application version to migrate. Add the build, source structure, persistence configuration, tests, and CI workflow through the normal branch/PR path. There are no business migrations in this change and no business schema is created. Roll back by reverting the change before any future production deployment; do not perform a production deployment as part of this plan.

## Open Questions

- Confirm the Java package root before creating application classes.
- Confirm which deployment environments need named profiles; otherwise use external property overrides and a deterministic test configuration without predefining environment names.
