package lab.springmvc.questions;

public class Q18ServletRequestThreadModelExample {

    record ThreadTransition(String phase, String threadName, boolean isContainerThread) {}

    public static void main(String[] args) {
        // Servlet 3.0+ async processing unbinds the initial container thread while worker executes
        ThreadTransition t1 = new ThreadTransition("Request Arrives", "http-nio-8080-exec-1", true);
        ThreadTransition t2 =
                new ThreadTransition("Async Processing", "async-worker-pool-4", false);
        ThreadTransition t3 =
                new ThreadTransition("Response Dispatched", "http-nio-8080-exec-9", true);

        boolean diffThreads = !t1.threadName().equals(t2.threadName()); // true
        boolean workerIsCustom = !t2.isContainerThread(); // true
        boolean finalDispatched = t3.isContainerThread(); // true

        System.out.println(
                "Container unbinds: "
                        + diffThreads
                        + ", worker thread decoupled: "
                        + workerIsCustom
                        + ", dispatched back: "
                        + finalDispatched);
    }
}
