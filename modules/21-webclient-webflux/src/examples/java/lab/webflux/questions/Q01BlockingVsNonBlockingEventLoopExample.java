package lab.webflux.questions;

public class Q01BlockingVsNonBlockingEventLoopExample {

    record ServerArchitecture(String model, int threadCount, boolean blocksCallingThread) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Spring MVC uses thread-per-request (Tomcat default 200 threads).
        // If a request waits 1 second for a downstream API, that worker thread is blocked and idle.
        ServerArchitecture mvc = new ServerArchitecture("Spring MVC (Tomcat)", 200, true);

        // Spring WebFlux uses an event loop (Netty default 1 thread per CPU core, e.g. 8 threads).
        // WebFlux threads never wait on I/O; they register epoll/kqueue selectors and process other
        // requests.
        ServerArchitecture webflux = new ServerArchitecture("Spring WebFlux (Netty)", 8, false);

        boolean mvcBlocks = mvc.blocksCallingThread(); // true
        boolean webfluxBlocks = webflux.blocksCallingThread(); // false

        System.out.println("Spring MVC thread blocks: " + mvcBlocks);
        System.out.println("Spring WebFlux thread blocks: " + webfluxBlocks);
    }
}
