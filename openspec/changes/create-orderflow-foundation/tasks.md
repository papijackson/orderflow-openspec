# Tasks

## 1. Maven and application baseline

- [x] 1.1 Confirm the Java base package and configuration strategy; record the selected package root and whether named environment profiles are needed in the application README, verifying that no environment-specific secrets or production endpoints are included.
- [x] 1.2 Add the Maven Wrapper and single-module Java 21 / Spring Boot 4 build with Boot-managed Web, Validation, Spring Data JPA, PostgreSQL, and Flyway dependencies plus the agreed test dependencies; verify `./mvnw -B -DskipTests compile` succeeds. (Build validation)
- [x] 1.3 Add the minimal Spring Boot entry point and only packages for responsibilities that exist; verify the application compiles without placeholder business controllers, services, repositories, or entities. (Layered architecture: Application responsibilities have explicit package boundaries)

## 2. Configuration, persistence, and architecture guardrails

- [x] 2.1 Externalize non-secret application and datasource settings, bind grouped settings with startup validation, and keep credentials outside tracked configuration; add tests for an environment override, missing/invalid required settings, and safe diagnostics, verifying the relevant configuration tests pass. (Application configuration: Environment-specific settings are externalized; Invalid required configuration prevents startup; Secrets are not embedded in application configuration)
- [x] 2.2 Configure PostgreSQL persistence and Flyway without creating business tables or using Hibernate schema generation for evolution; add a PostgreSQL Testcontainers startup/migration test and verify it passes against the migrated schema. (Application configuration: Environment-specific settings are externalized)
- [x] 2.3 Add ArchUnit rules for the declared Controller, Service, Repository, and Entity dependency boundaries; verify architecture tests pass and identify a forbidden dependency when exercised by a controlled test fixture. (Layered application architecture: Layer dependencies respect application responsibilities; Build validation: Architecture rules are part of automated validation)

## 3. REST error contract

- [x] 3.1 Implement centralized RFC 9457 `ProblemDetail` error handling for request validation and unexpected failures; add web tests that assert `application/problem+json`, matching HTTP/problem status, HTTP 400 for invalid input, generic HTTP 500 for unexpected failures, and absence of sensitive/internal details. Verify the focused web tests pass. (REST error contract: Handled REST errors use a consistent problem representation; Invalid REST input returns a safe client error; Unexpected REST failures do not expose internals)

## 4. Continuous integration and integrated verification

- [ ] 4.1 Add a GitHub Actions workflow for pull requests and pushes that configures Java 21, uses the Maven Wrapper, runs `./mvnw -B verify`, and contains no deployment steps or secrets; verify the workflow triggers and command, then confirm a successful CI run reports the required checks as passing. (Build validation: Pull requests and pushes receive build and test validation; Architecture rules are part of automated validation; Build validation does not deploy the application)
- [x] 4.2 Run `./mvnw -B verify` across the complete implementation and record its code/test result separately from the GitHub Actions run; resolve any failures without weakening the requirements. (Build validation: Pull requests and pushes receive build and test validation)

## Workflow follow-up

- Run `openspec validate "create-orderflow-foundation"` to validate the planning artifacts. This result is independent of, and does not establish, successful compilation or tests; code validation is covered by the Maven and CI tasks above.
