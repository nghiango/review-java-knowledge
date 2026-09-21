package lab.designpatterns.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpecificationPatternTest {

    @Test
    @DisplayName("Composable specifications evaluate AND, OR, and NOT predicates correctly")
    void specifications_composeAsExpected() {
        Specification<Product> inStock = new InStockSpecification();
        Specification<Product> premium = new PremiumProductSpecification();

        Specification<Product> inStockPremium = inStock.and(premium);
        Specification<Product> outOfStockOrBudget = inStock.not().or(premium.not());

        Product macBook = new Product("MB-1", "MacBook Pro", new BigDecimal("2499.00"), 10, true);
        Product cheapCable =
                new Product("CAB-1", "USB-C Cable", new BigDecimal("15.00"), 100, true);
        Product outOfStockServer =
                new Product("SRV-1", "Rack Server", new BigDecimal("5000.00"), 0, true);

        assertThat(inStockPremium.isSatisfiedBy(macBook)).isTrue();
        assertThat(inStockPremium.isSatisfiedBy(cheapCable)).isFalse(); // Not premium
        assertThat(inStockPremium.isSatisfiedBy(outOfStockServer)).isFalse(); // Not in stock

        assertThat(outOfStockOrBudget.isSatisfiedBy(cheapCable)).isTrue();
        assertThat(outOfStockOrBudget.isSatisfiedBy(outOfStockServer)).isTrue();
        assertThat(outOfStockOrBudget.isSatisfiedBy(macBook)).isFalse();
    }
}
