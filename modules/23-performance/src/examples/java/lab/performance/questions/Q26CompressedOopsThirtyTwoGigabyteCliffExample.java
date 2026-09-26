package lab.performance.questions;

/**
 * Demonstrates JVM Compressed Ordinary Object Pointers (-XX:+UseCompressedOops) and the 32GB heap boundary cliff,
 * where exceeding 32GB disables pointer compression and doubles reference sizes from 32-bit to 64-bit.
 */
public final class Q26CompressedOopsThirtyTwoGigabyteCliffExample {
    public static void main(String[] args) {
        long heapSizeBytes31Gb = 31L * 1024 * 1024 * 1024;
        long heapSizeBytes33Gb = 33L * 1024 * 1024 * 1024;

        boolean compressedOopsActive31Gb = heapSizeBytes31Gb < (32L * 1024 * 1024 * 1024); // true
        boolean compressedOopsActive33Gb = heapSizeBytes33Gb < (32L * 1024 * 1024 * 1024); // false

        System.out.println("Compressed OOPs enabled at 31GB: " + compressedOopsActive31Gb);
        System.out.println("Compressed OOPs disabled at 33GB (cliff): " + !compressedOopsActive33Gb);
    }
}
