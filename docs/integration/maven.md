# Maven Integration

The kit intentionally does not overwrite an existing `pom.xml`.

Recommended test dependencies, when not already present:

- JUnit Jupiter
- Mockito
- AssertJ
- Spring Boot Test
- Testcontainers PostgreSQL
- Cucumber
- ArchUnit
- JaCoCo

Recommended production dependencies where applicable:

- Flyway
- Spring Boot Actuator
- Micrometer Prometheus registry
- AWS SDK modules required by the application

## Quality Commands

Typical convergence:

```bash
./mvnw -B verify
```

Optional SonarQube:

```bash
./mvnw -B sonar:sonar \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN"
```

Do not add dependencies mechanically. Reuse the existing dependency management and Spring Boot BOM.
