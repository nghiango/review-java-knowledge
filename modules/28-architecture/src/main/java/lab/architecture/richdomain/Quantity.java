package lab.architecture.richdomain;

public record Quantity(int value) {

    public Quantity {
        if (value <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }
}
