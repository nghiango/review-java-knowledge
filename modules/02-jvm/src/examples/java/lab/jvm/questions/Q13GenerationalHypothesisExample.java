package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q13GenerationalHypothesisExample {
    private Q13GenerationalHypothesisExample() {}

    public static void main(String[] args) {
        // Short-lived objects die in Eden (Young Gen) during Minor GC
        for (int i = 0; i < 1000; i++) {
            String temp = new String("temp-" + i);
        }

        // Long-lived objects survive multiple GC cycles and tenure into Old Generation
        String permanentTenant = "tenant-long-lived"; // tenured / permanent
    }
}
