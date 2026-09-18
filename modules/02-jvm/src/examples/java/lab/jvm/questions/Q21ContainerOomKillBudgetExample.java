package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q21ContainerOomKillBudgetExample {
    private Q21ContainerOomKillBudgetExample() {}

    public static void main(String[] args) {
        long containerRam = 1024L * 1024 * 1024; // 1 GB container limit
        long configuredHeap = 650L * 1024 * 1024; // -XX:MaxRAMPercentage=65% (~650 MB)
        long nativeBudget =
                250L * 1024 * 1024; // Metaspace + CodeCache + Thread Stacks + Direct (~250 MB)
        long osHeadroom = 124L * 1024 * 1024; // OS + glibc malloc buffer (~124 MB)

        long totalEstimatedRss =
                configuredHeap + nativeBudget; // 900 MB (safely below 1 GB cgroup memory threshold)
    }
}
