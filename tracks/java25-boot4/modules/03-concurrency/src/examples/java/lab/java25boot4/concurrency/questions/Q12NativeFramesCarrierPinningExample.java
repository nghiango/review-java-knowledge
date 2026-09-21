package lab.java25boot4.concurrency.questions;

public class Q12NativeFramesCarrierPinningExample {

    public static void main(String[] args) {
        // While synchronized blocks are unpinned in Java 25, native JNI calls on the call stack
        // still pin virtual threads to carrier threads during blocking I/O.
        boolean nativeFramePinsCarrier = true;
        boolean synchronizedPinsCarrier = false;

        System.out.println(
                "Java 25 native JNI blocking pins carrier: "
                        + nativeFramePinsCarrier); // Java 25 native JNI blocking pins carrier: true
        System.out.println(
                "Java 25 synchronized blocks pin carrier: "
                        + synchronizedPinsCarrier); // Java 25 synchronized blocks pin carrier:
        // false
    }
}
