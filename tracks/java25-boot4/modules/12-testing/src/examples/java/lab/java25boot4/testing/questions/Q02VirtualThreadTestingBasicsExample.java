package lab.java25boot4.testing.questions;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Q02: How do virtual threads affect test design and thread lifecycle tracking in Java 25? */
public class Q02VirtualThreadTestingBasicsExample {

    public static void main(String[] args) throws Exception {
        AtomicBoolean ranOnVirtual = new AtomicBoolean(false);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(
                            () -> {
                                ranOnVirtual.set(Thread.currentThread().isVirtual());
                            })
                    .get();
        }

        System.out.println(
                "Ran on virtual thread: " + ranOnVirtual.get()); // Ran on virtual thread: true
    }
}
