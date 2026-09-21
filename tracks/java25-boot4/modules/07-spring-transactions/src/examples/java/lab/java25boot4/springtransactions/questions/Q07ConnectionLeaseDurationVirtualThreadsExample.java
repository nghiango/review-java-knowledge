package lab.java25boot4.springtransactions.questions;

public class Q07ConnectionLeaseDurationVirtualThreadsExample {

    public static void main(String[] args) {
        // High connection lease duration starves the pool when thousands of virtual threads block.
        // Golden rule: Acquire connection as late as possible, release as early as possible.
        boolean keepConnectionHoldMinimal = true;
        System.out.println(
                "Minimize lease duration: "
                        + keepConnectionHoldMinimal); // Minimize lease duration: true
    }
}
