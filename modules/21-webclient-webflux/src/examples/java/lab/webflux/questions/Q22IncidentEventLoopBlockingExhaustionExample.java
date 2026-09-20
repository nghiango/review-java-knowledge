package lab.webflux.questions;

public class Q22IncidentEventLoopBlockingExhaustionExample {

    record IncidentReport(
            String symptom, int availableEventLoops, int blockedThreads, String rootCause) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Incident: High latency in downstream auth service freezes all routes on a WebFlux
        // gateway.
        // Root Cause: A legacy authentication filter called a synchronous LDAP client or .block()
        // on the Netty event loop.
        // Because the gateway runs only 8 event loops (1 per CPU core), 8 concurrent slow requests
        // blocked all 8 threads.
        // Remediations:
        // 1. Offload blocking calls to Schedulers.boundedElastic().
        // 2. Add BlockHound in CI/CD pipeline to reject any blocking calls on NonBlocking threads.
        IncidentReport report =
                new IncidentReport(
                        "Gateway 504 Gateway Timeout across all endpoints",
                        8,
                        8,
                        "Synchronous .block() inside AuthenticationWebFilter on reactor-http-epoll thread");

        boolean totalStarvation = report.availableEventLoops() == report.blockedThreads(); // true
        int activeThreads = report.availableEventLoops() - report.blockedThreads(); // 0

        System.out.println("All event loops frozen: " + totalStarvation);
        System.out.println("Remaining active worker loops: " + activeThreads);
    }
}
