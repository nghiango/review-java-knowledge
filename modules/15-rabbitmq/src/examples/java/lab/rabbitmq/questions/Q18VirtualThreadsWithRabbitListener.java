package lab.rabbitmq.questions;

import java.util.concurrent.Executors;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;

/** Q18: How do Java 21 Virtual Threads scale Spring AMQP SimpleMessageListenerContainer? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q18VirtualThreadsWithRabbitListener {

    public static void main(String[] args) {
        // SimpleMessageListenerContainer executes listener methods using a TaskExecutor.
        // In high-concurrency environments performing blocking downstream DB or HTTP calls,
        // kernel thread pools (e.g. 50-200 platform threads) consume heavy stack memory (~1MB
        // each).
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        // With Java 21, assign Executors.newVirtualThreadPerTaskExecutor() as the task executor:
        var virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        factory.setTaskExecutor(virtualExecutor);

        // Allows scaling concurrency to thousands of concurrent listener tasks with negligible
        // memory overhead
        boolean zeroPlatformThreadStarvation = true; // true
        virtualExecutor.close();
    }
}
