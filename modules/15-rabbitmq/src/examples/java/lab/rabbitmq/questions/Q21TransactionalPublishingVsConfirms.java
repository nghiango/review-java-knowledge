package lab.rabbitmq.questions;

/**
 * Q21: Compare AMQP transactions (tx_select / tx_commit) versus asynchronous publisher confirms.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q21TransactionalPublishingVsConfirms {

    public static void main(String[] args) {
        // AMQP Transactions (channel.txSelect() / channel.txCommit()):
        // Synchronous blocking protocol.
        // On every commit, client blocks waiting for synchronous network round-trip and broker disk
        // fsync.
        // Causes severe throughput degradation: typically drops publishing throughput by up to
        // 250x!
        boolean amqpTransactionsAreSynchronousBlocking = true; // true

        // Publisher Confirms (channel.confirmSelect()):
        // Completely asynchronous pipelining.
        // Client streams thousands of messages continuously with monotonically increasing sequence
        // numbers.
        // Broker asynchronously responds with basic.ack (or basic.nack) containing sequence numbers
        // and multiple flag.
        // Delivers 100x-200x higher throughput while providing the same durability guarantees!
        boolean confirmsAreAsynchronousAndHighThroughput = true; // true
    }
}
