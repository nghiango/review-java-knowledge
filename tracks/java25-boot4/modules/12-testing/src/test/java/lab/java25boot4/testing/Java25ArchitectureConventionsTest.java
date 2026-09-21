package lab.java25boot4.testing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Java25ArchitectureConventionsTest {

    private final JavaClasses importedClasses =
            new ClassFileImporter().importPackages("lab.java25boot4.testing");

    @Test
    @DisplayName(
            "ArchUnit rule: all classes must reside in lab.java25boot4.testing package hierarchy")
    void classesShouldResideInCorrectPackage() {
        ArchRule rule =
                classes()
                        .should()
                        .resideInAPackage("lab.java25boot4.testing..")
                        .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("ArchUnit rule: production classes must not depend on broken examples")
    void productionClassesMustNotDependOnBrokenExamples() {
        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("lab.java25boot4.testing")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage("..broken..")
                        .allowEmptyShould(true);

        rule.check(importedClasses);
    }
}
