package com.orderflow.architecture;

import com.orderflow.controller.fixture.ForbiddenControllerFixture;
import com.orderflow.repository.fixture.RepositoryFixture;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

@AnalyzeClasses(packages = "com.orderflow", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerArchitectureTest {

    @ArchTest
    static final ArchRule controllersMustNotDependOnRepositories = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule servicesMustNotDependOnWebComponents = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web..", "org.springframework.http..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositoriesMustNotDependOnApplicationLayers = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..controller..", "..service..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesMustNotDependOnApplicationOrInfrastructure = noClasses()
            .that().resideInAPackage("..entity..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "..controller..",
                    "..service..",
                    "..repository..",
                    "..configuration..",
                    "..client..",
                    "..messaging..",
                    "..batch..",
                    "org.springframework..",
                    "software.amazon.awssdk..")
            .allowEmptyShould(true);

    @Test
    void shouldIdentifyForbiddenControllerRepositoryDependency() {
        var fixtureClasses = new ClassFileImporter()
                .importClasses(ForbiddenControllerFixture.class, RepositoryFixture.class);

        var result = controllersMustNotDependOnRepositories.evaluate(fixtureClasses);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().toString())
                .contains("ForbiddenControllerFixture", "RepositoryFixture");
    }
}
