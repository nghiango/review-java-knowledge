package lab.jvm.examples;

public final class AllocationDemo {
    private static final int DEFAULT_ITERATIONS = 100_000;

    private AllocationDemo() {}

    public static void main(String[] args) {
        int iterations = args.length == 0 ? DEFAULT_ITERATIONS : Integer.parseInt(args[0]);
        long checksum = 0;
        for (int index = 0; index < iterations; index++) {
            checksum += new Sample("request-" + index, index).encodedLength();
        }
        System.out.printf("iterations=%d checksum=%d%n", iterations, checksum);
    }

    private record Sample(String key, int value) {
        int encodedLength() {
            return key.length() + Integer.toString(value).length();
        }
    }
}
