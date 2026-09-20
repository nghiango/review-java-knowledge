package lab.testing.questions;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * Q16: ArchUnit rules and package dependencies.
 *
 * <p>ArchUnit imports the compiled classes (ASM reads the constant pool, so every referenced type
 * is a dependency) and then evaluates rules over that graph. A rule reads as a sentence: {@code
 * noClasses().that().resideInAPackage("..pricing..").should().dependOnClassesThat()
 * .resideInAPackage("..accounts..")} — "no classes that reside in a package '..pricing..' should
 * depend on classes that reside in a package '..accounts..'". A rule like that catches the import
 * which quietly couples two packages, something no behavioural test notices. {@code
 * FreezingArchRule} records today's violations so a legacy codebase can adopt a rule and fail only
 * on *new* ones.
 *
 * <p>The three rules below are the canonical shape: one {@code @AnalyzeClasses} declaration for the
 * import, one {@code @ArchTest} field per rule. The example never imports bytecode — {@code main}
 * only builds the rules and reads their types back.
 */
@AnalyzeClasses(packages = "lab.testing", importOptions = ImportOption.DoNotIncludeTests.class)
public class Q16ArchunitRulesAndPackageDependencies {

    /** The rule the module's real graph satisfies: pricing does not know about accounts. */
    @ArchTest
    static final ArchRule pricingMustNotDependOnAccounts =
            noClasses()
                    .that()
                    .resideInAPackage("..pricing..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..accounts..");

    /** The rule a new adapter breaks: orders must not reach into accounts either. */
    @ArchTest
    static final ArchRule ordersMustNotDependOnAccounts =
            noClasses()
                    .that()
                    .resideInAPackage("..orders..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..accounts..");

    /** A layered rule states the allowed direction once, instead of one rule per package pair. */
    @ArchTest
    static final LayeredArchitecture layeredDependencies =
            layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Pricing")
                    .definedBy("..pricing..")
                    .layer("Orders")
                    .definedBy("..orders..")
                    .layer("Accounts")
                    .definedBy("..accounts..")
                    .whereLayer("Accounts")
                    .mayNotBeAccessedByAnyLayer();

    public static void main(String[] args) {
        // The rules are objects; only a bytecode import makes them evaluable, and that is the
        // engine's job when it runs the @ArchTest fields. Nothing here reads a class file.
        ArchRule pricingRule = pricingMustNotDependOnAccounts;
        ArchRule ordersRule = ordersMustNotDependOnAccounts;
        LayeredArchitecture layers = layeredDependencies;
        ArchRule frozen =
                FreezingArchRule.freeze(ordersRule); // today's violations become a baseline
        GivenClasses given = classes(); // the entry point of every class rule

        String ruleType = pricingRule.getClass().getSimpleName(); // the ArchRule implementation
        String givenType = given.getClass().getSimpleName(); // the GivenClasses implementation
        String layeredType = layers.getClass().getSimpleName(); // "LayeredArchitecture"
        String frozenType = frozen.getClass().getSimpleName(); // "FreezingArchRule"
        String analysisAnnotation = AnalyzeClasses.class.getSimpleName(); // "AnalyzeClasses"
        String ruleAnnotation = ArchTest.class.getSimpleName(); // "ArchTest"

        String ruleText = pricingRule.getDescription(); // the rule as the sentence quoted above
        boolean ruleNamesBothPackages =
                ruleText.contains("..pricing..") && ruleText.contains("..accounts.."); // true
        String layerText = layers.getDescription();
        boolean layersNameTheAccountsLayer =
                layerText.contains("Pricing") && layerText.contains("Accounts"); // true

        System.out.println("Rule type: " + ruleType); // Rule type: ClassesShouldInternal
        System.out.println("Given type: " + givenType); // Given type: GivenClassesInternal
        System.out.println("Layered type: " + layeredType); // Layered type: LayeredArchitecture
        System.out.println("Frozen type: " + frozenType); // Frozen type: FreezingArchRule
        System.out.println("Analysis: " + analysisAnnotation); // Analysis: AnalyzeClasses
        System.out.println("ArchTest: " + ruleAnnotation); // ArchTest: ArchTest
        System.out.println(
                "Rule names packages: " + ruleNamesBothPackages); // Rule names packages: true
        System.out.println("Layers named: " + layersNameTheAccountsLayer); // Layers named: true
    }
}
