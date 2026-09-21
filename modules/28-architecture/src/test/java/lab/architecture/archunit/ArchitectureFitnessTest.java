package lab.architecture.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architectural fitness function tests using ArchUnit.
 * Automatically verifies Hexagonal boundaries, modular monolith slices, and code conventions.
 */
class ArchitectureFitnessTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("lab.architecture");
    }

    @Test
    @DisplayName("Clean Architecture: Domain model must not depend on infrastructure, application, Spring or JPA")
    void domainMustNotDependOnInfrastructureOrFrameworks() {
        noClasses()
                .that().resideInAPackage("..cleanarchitecture.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..cleanarchitecture.infrastructure..",
                        "..cleanarchitecture.application..",
                        "org.springframework..",
                        "jakarta.persistence.."
                )
                .because("Pure domain models must be free of framework and infrastructure dependencies")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Clean Architecture: Application service must not depend on infrastructure adapters")
    void applicationServiceMustNotDependOnInfrastructureAdapters() {
        noClasses()
                .that().resideInAPackage("..cleanarchitecture.application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..cleanarchitecture.infrastructure.adapter..")
                .because("Application layer should only depend on domain ports, not concrete infrastructure adapters")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Modular Monolith: Modules must not access internal packages of other modules")
    void modulesMustNotAccessOtherModulesInternalPackages() {
        noClasses()
                .that().resideInAPackage("..modularmonolith.billing..")
                .should().dependOnClassesThat()
                .resideInAPackage("..modularmonolith.inventory.internal..")
                .because("Billing module must interact with Inventory only via its public InventoryApi")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Coding Rules: No field injection with @Autowired")
    void noFieldInjection() {
        fields()
                .that().areDeclaredInClassesThat().resideInAPackage("lab.architecture..")
                .should().notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .because("Constructor injection must always be used over field injection")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Coding Rules: No java.util.logging; use SLF4J")
    void noJavaUtilLogging() {
        noClasses()
                .that().resideInAPackage("lab.architecture..")
                .should().dependOnClassesThat().resideInAPackage("java.util.logging..")
                .because("SLF4J must be used for logging")
                .check(importedClasses);
    }
}
