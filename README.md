# OrderFlow

OrderFlow is a Java 21 / Spring Boot 4 application following the repository's
specification-driven development workflow.

## Architecture

The application uses a single Maven module and the layered structure described
in `.github/instructions/architecture.instructions.md`. The initial Java
package root is `com.orderflow`; add layer packages only when their
responsibilities exist. Do not introduce business classes or additional
architectural layers without a corresponding requirement.

## Configuration

Common non-secret defaults belong in application configuration. Supply
environment-specific values through Spring Boot external configuration (for
example, environment variables); keep credentials and secret values out of
tracked files. A separate Spring profile is not needed until an environment
requires behavior that differs from the common configuration. Tests use
deterministic test configuration.

PostgreSQL connection settings are supplied externally using Spring Boot's
standard datasource properties. Database schema changes are managed by
versioned Flyway migrations; Hibernate is not used to evolve the schema.

## Build and tests

Use the Maven Wrapper to compile, test, and run the project's quality checks:

```bash
./mvnw -B verify
```

GitHub Actions runs build and test validation for pushes and pull requests.
The workflow does not deploy the application.
