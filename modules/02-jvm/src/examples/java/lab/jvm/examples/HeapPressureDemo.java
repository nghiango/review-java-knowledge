package lab.jvm.examples;

import java.util.ArrayList;
import java.util.List;

public final class HeapPressureDemo {
    private static final String CONFIRMATION = "--i-understand";
    private static final int MIB = 1024 * 1024;

    private HeapPressureDemo() {}

    public static void main(String[] args) {
        if (!confirmed(args)) {
            System.out.printf(
                    "Usage: java -Xmx64m %s %s%n", HeapPressureDemo.class.getName(), CONFIRMATION);
            return;
        }
        List<byte[]> retained = new ArrayList<>();
        while (true) {
            retained.add(new byte[MIB]);
            if (retained.size() % 8 == 0) {
                System.out.printf("retained approximately %d MiB%n", retained.size());
            }
        }
    }

    private static boolean confirmed(String[] args) {
        return args.length == 1 && CONFIRMATION.equals(args[0]);
    }
}
