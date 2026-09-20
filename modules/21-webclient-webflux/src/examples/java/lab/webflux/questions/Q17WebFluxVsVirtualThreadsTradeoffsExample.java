package lab.webflux.questions;

public class Q17WebFluxVsVirtualThreadsTradeoffsExample {

    record ConcurrencyParadigm(
            String name,
            String paradigm,
            String primaryUseCase,
            boolean supportsBlockingLibraries) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Java 21 Virtual Threads (Spring MVC) vs Spring WebFlux:
        // - Virtual Threads allow writing simple synchronous imperative code (JDBC, JPA,
        // RestClient) with massive concurrency.
        // - WebFlux is required for true end-to-end streaming (Server-Sent Events, WebSockets,
        // reactive microservice mesh)
        //   or when operating with non-blocking drivers and complex reactive compositions
        // (combineLatest, window, sample).
        // - WebFlux should be avoided when the system is heavily bound to blocking relational
        // databases (JPA/Hibernate)
        //   or when team cognitive load with reactive streams leads to subtle blocking event loop
        // bugs.
        ConcurrencyParadigm mvcWithVirtualThreads =
                new ConcurrencyParadigm(
                        "Spring MVC + Virtual Threads",
                        "Synchronous Imperative",
                        "CRUD, JPA, JDBC, REST microservices",
                        true);

        ConcurrencyParadigm webflux =
                new ConcurrencyParadigm(
                        "Spring WebFlux",
                        "Asynchronous Reactive Streams",
                        "High-throughput streaming, SSE, WebSockets, gateway proxies",
                        false);

        boolean mvcSupportsBlocking = mvcWithVirtualThreads.supportsBlockingLibraries(); // true
        boolean webfluxEnforcesNonBlocking = !webflux.supportsBlockingLibraries(); // true

        System.out.println(
                "Spring MVC + Virtual Threads supports blocking JDBC/JPA: " + mvcSupportsBlocking);
        System.out.println(
                "Spring WebFlux requires non-blocking ecosystem: " + webfluxEnforcesNonBlocking);
    }
}
