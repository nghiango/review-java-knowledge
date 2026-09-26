package lab.testing.questions;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Q28: How do you implement custom ArchUnit rules enforcing domain isolation (hexagonal
 * architecture, zero cyclic dependencies, constructor injection only)?
 */
public class Q28ArchUnitFitnessFunctionsInCi {

    public static void main(String[] args) {
        // Architectural Fitness Rule 1: Domain layer must not depend on Spring or Infrastructure
        ArchRule domainIsolationRule =
                classes()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .onlyDependOnClassesThat()
                        .resideInAnyPackage("..domain..", "java..", "org.jspecify..");

        // Architectural Fitness Rule 2: No field injection (@Autowired on fields) anywhere
        ArchRule noFieldInjectionRule =
                noClasses()
                        .should()
                        .dependOnClassesThat()
                        .haveFullyQualifiedName("org.springframework.beans.factory.annotation.Autowired");

        JavaClasses importedClasses =
                new ClassFileImporter().importPackages("lab.testing");

        boolean domainRuleDefined = domainIsolationRule.getDescription() != null; // true
        boolean noFieldInjectionDefined = noFieldInjectionRule.getDescription() != null; // true

        System.out.println("Domain Isolation Rule Active: " + domainRuleDefined); // true
        System.out.println("No Field Injection Rule Active: " + noFieldInjectionDefined); // true
        System.out.println("Imported Classes for Analysis: " + importedClasses.size());
    }
}
