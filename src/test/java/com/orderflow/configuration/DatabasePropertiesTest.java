package com.orderflow.configuration;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class DatabasePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DatabasePropertiesTestConfiguration.class);

    @Test
    void shouldUseExternallySuppliedDatasourceUrl() {
        contextRunner
                .withPropertyValues("spring.datasource.url=")
                .withInitializer(context -> context.getEnvironment()
                        .getPropertySources()
                        .addFirst(new SystemEnvironmentPropertySource(
                                "deployment",
                                Map.of("SPRING_DATASOURCE_URL", "jdbc:postgresql://db.example:5432/orderflow"))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(DatabaseProperties.class).url())
                            .isEqualTo("jdbc:postgresql://db.example:5432/orderflow");
                });
    }

    @Test
    void shouldRejectMissingDatasourceUrlWithoutDisclosingCredentials() {
        contextRunner
                .withPropertyValues(
                        "spring.datasource.url=",
                        "spring.datasource.password=synthetic-secret")
                .run(context -> {
                    assertThat(context).hasFailed();
                    var startupFailure = context.getStartupFailure();
                    assertThat(startupFailure).isNotNull();
                    assertThat(exceptionMessages(startupFailure))
                            .contains("spring.datasource.url")
                            .doesNotContain("synthetic-secret");
                });
    }

    @Test
    void shouldRejectDatasourceUrlThatIsNotPostgresql() {
        contextRunner
                .withPropertyValues("spring.datasource.url=https://db.example/orderflow")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(exceptionMessages(context.getStartupFailure()))
                            .contains("url", "PostgreSQL JDBC URL");
                });
    }

    private static String exceptionMessages(Throwable failure) {
        var messages = new StringBuilder();
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append('\n');
        }
        return messages.toString();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DatabaseProperties.class)
    private static class DatabasePropertiesTestConfiguration {
    }
}
