package lab.corejava.questions;

@SuppressWarnings("unused")
public final class Q28FalseSharingPaddingExample {
    private Q28FalseSharingPaddingExample() {}

    // False sharing hazard: core1 writing to valueA invalidates core2's L1/L2 cache line holding
    // valueB
    public static class UnpaddedValues {
        public volatile long valueA = 0L;
        public volatile long valueB = 0L; // Sits on the same 64-byte L1 CPU cache line
    }

    // Padded structure: forces fields onto distinct CPU cache lines (64 bytes = 8 longs)
    public static class PaddedValue {
        // Pre-padding
        public long p1, p2, p3, p4, p5, p6, p7;
        public volatile long targetValue = 0L; // Isolated on its own 64-byte line
        // Post-padding
        public long p8, p9, p10, p11, p12, p13, p14;
    }

    public static void main(String[] args) {
        UnpaddedValues unpadded = new UnpaddedValues();
        unpadded.valueA = 100L;
        unpadded.valueB = 200L;

        PaddedValue padded = new PaddedValue();
        padded.targetValue = 42L;

        // LongAdder internally uses @jdk.internal.vm.annotation.Contended on its Cell array
        // to avoid this exact cache-line false sharing penalty under high concurrency.
    }
}
