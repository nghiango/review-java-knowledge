package lab.java25boot4.springtransactions.questions;

public class Q03PropagationAcrossThreadsExample {

    public static void main(String[] args) {
        // Propagation.REQUIRED or REQUIRES_NEW applies strictly to the calling thread.
        // It does NOT propagate across threads or executors.
        boolean propagatesAcrossThreads = false;
        System.out.println(
                "Spring tx propagates across threads: "
                        + propagatesAcrossThreads); // Spring tx propagates across threads: false
    }
}
