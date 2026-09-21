package lab.designpatterns.specification;

public class InStockSpecification implements Specification<Product> {

    @Override
    public boolean isSatisfiedBy(Product product) {
        return product != null && product.active() && product.stockQuantity() > 0;
    }
}
