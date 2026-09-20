package lab.testing.questions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lab.testing.pricing.Money;
import lab.testing.pricing.PricingCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Q10: JUnit 5 extensions and parameterized tests.
 *
 * <p>{@code @ParameterizedTest} is not a kind of {@code @Test}: it is a {@link TestTemplate}
 * extended by {@code ParameterizedTestExtension}, and each row of its source becomes one test
 * invocation with its own lifecycle. An {@link Extension} is a class the engine calls back at the
 * lifecycle points it implements — that is how Mockito, Spring and Testcontainers hook in without
 * the test doing anything. The nested classes below show the shapes; this example prints the counts
 * they produce instead of running the engine.
 */
public class Q10Junit5ExtensionsAndParameterizedTests {

    /** One row per {@code @CsvSource} invocation: unit price, quantity, discount percent, cents. */
    private static final String[] DISCOUNT_CASES = {
        "1250, 1, 33, 838", "1999, 2, 20, 3198", "500, 1, 100, 0", "250, 3, 0, 750"
    };

    /** The shape of a parameterized pricing test: one method, one invocation per row. */
    @Tag("unit")
    static class DiscountPricingTest {

        // JUnit creates a fresh instance per test method by default, so instance fields are
        // per-test state and never a leak between methods.
        private final PricingCalculator calculator = new PricingCalculator();

        @ParameterizedTest(name = "unit price {0} x {1} at {2}% costs {3} cents")
        @CsvSource({"1250, 1, 33, 838", "1999, 2, 20, 3198", "500, 1, 100, 0", "250, 3, 0, 750"})
        @DisplayName("a basket is priced and discounted")
        void checkout_discountRounding_returnsExpectedCents(
                long unitPriceCents, int quantity, int percent, long expectedCents) {
            Money subtotal = new Money(unitPriceCents).multiply(quantity);
            Money discounted = calculator.applyPercentDiscount(subtotal, percent);

            assertThat(discounted).isEqualTo(new Money(expectedCents));
        }

        @ParameterizedTest
        @MethodSource("discountCases")
        void discount_fromAMethodSource_isApplied(Money amount, int percent, Money expected) {
            assertThat(calculator.applyPercentDiscount(amount, percent)).isEqualTo(expected);
        }

        /** A provider: the parameterized engine calls it once per test method invocation. */
        static Stream<Arguments> discountCases() {
            return Stream.of(
                    Arguments.of(new Money(1250), 33, new Money(838)),
                    Arguments.of(new Money(1999), 20, new Money(1599)));
        }
    }

    /** An extension is a class the engine calls at each lifecycle point it implements. */
    static final class RecordingExtension implements BeforeEachCallback, AfterEachCallback {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void beforeEach(ExtensionContext context) {
            calls.add("before:" + context.getRequiredTestMethod().getName());
        }

        @Override
        public void afterEach(ExtensionContext context) {
            calls.add("after:" + context.getRequiredTestMethod().getName());
        }

        List<String> calls() {
            return List.copyOf(calls);
        }
    }

    /** Registering an extension is what gives a test its framework hooks. */
    @ExtendWith(RecordingExtension.class)
    static class ExtendableTest {

        @Test
        void moneyAddition_isExact() {
            assertThat(new Money(1).plus(new Money(1))).isEqualTo(new Money(2));
        }
    }

    /** A nested class shares the outer lifecycle and can add its own @BeforeEach/@AfterEach. */
    @Nested
    @DisplayName("discount rules")
    class DiscountRules {

        @BeforeEach
        void setUp() {
            // Runs before every test in this nested class, and after the outer @BeforeEach methods.
        }

        @AfterEach
        void tearDown() {
            // Runs after every test in this nested class, and before the outer @AfterEach methods.
        }

        @Test
        @DisplayName("a zero discount leaves the amount unchanged")
        void applyPercentDiscount_zeroPercent_returnsTheSameAmount() {
            assertThat(new PricingCalculator().applyPercentDiscount(new Money(4748), 0))
                    .isEqualTo(new Money(4748));
        }
    }

    public static void main(String[] args) {
        int csvInvocations = DISCOUNT_CASES.length; // 4 invocations from one @ParameterizedTest
        int methodSourceCases = 2; // two Arguments in the provider

        boolean parameterizedIsATestTemplate =
                ParameterizedTest.class.isAnnotationPresent(TestTemplate.class); // true
        Class<? extends Extension> parameterizedExtension =
                ParameterizedTest.class.getAnnotation(ExtendWith.class)
                        .value()[0]; // the engine hook
        Class<? extends Extension> registered =
                ExtendableTest.class.getAnnotation(ExtendWith.class).value()[0];
        String builtInExtension =
                parameterizedExtension.getSimpleName(); // "ParameterizedTestExtension"
        String registeredExtension = registered.getSimpleName(); // "RecordingExtension"
        String nestedName = DiscountRules.class.getSimpleName(); // "DiscountRules"

        System.out.println("Csv invocations: " + csvInvocations); // Csv invocations: 4
        System.out.println("Method source cases: " + methodSourceCases); // Method source cases: 2
        System.out.println("TestTemplate: " + parameterizedIsATestTemplate); // TestTemplate: true
        System.out.println("Hook: " + builtInExtension); // Hook: ParameterizedTestExtension
        System.out.println("Ext: " + registeredExtension); // Ext: RecordingExtension
        System.out.println("Nested: " + nestedName); // Nested: DiscountRules
    }
}
