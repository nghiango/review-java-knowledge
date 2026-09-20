package lab.concurrency.broken.volatilecompound;

public class ItemStock {
    private final String itemId;
    private volatile int availableQuantity;

    public ItemStock(String itemId, int initialQuantity) {
        this.itemId = itemId;
        this.availableQuantity = initialQuantity;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
