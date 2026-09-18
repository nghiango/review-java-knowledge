package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q03StackVsHeapExample {
    private Q03StackVsHeapExample() {}

    public static class OrderItem {
        public final String sku;

        public OrderItem(String sku) {
            this.sku = sku;
        }
    }

    public static void main(String[] args) {
        int localPrimitive = 42; // allocated inside main thread's Stack Frame
        OrderItem heapObject =
                new OrderItem("ITEM-99"); // object allocated on Heap; reference stored on Stack

        String sku = heapObject.sku; // "ITEM-99"
    }
}
