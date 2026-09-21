package lab.designpatterns.specification;

import java.math.BigDecimal;

public class PremiumProductSpecification implements Specification<Product> {

    private static final BigDecimal THRESHOLD = new BigDecimal("500.00");

    @Override
    public boolean isSatisfiedBy(Product product) {
        return product != null && product.price().compareTo(THRESHOLD) >= 0;
    }
}
