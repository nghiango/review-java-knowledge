package lab.springboot.questions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Q16VirtualThreadsConfigurationExample {

    public static void main(String[] args) throws Exception {
        // When spring.threads.virtual.enabled=true, Spring Boot configures Tomcat and Async task
        // executors with Virtual Thread Per Task
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var future =
                    virtualExecutor.submit(
                            () -> {
                                boolean isVirtual = Thread.currentThread().isVirtual(); // true
                                return isVirtual;
                            });

            boolean executedOnVirtualThread = future.get(); // true

            System.out.println("Executed on virtual thread: " + executedOnVirtualThread);
        }
    }
}
