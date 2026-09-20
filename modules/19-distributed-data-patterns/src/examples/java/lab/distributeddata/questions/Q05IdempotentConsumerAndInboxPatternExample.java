package lab.distributeddata.questions;

import java.util.HashSet;
import java.util.Set;

public class Q05IdempotentConsumerAndInboxPatternExample {

    record InboxStore(Set<String> processedMessageIds) {
        boolean processMessage(String messageId) {
            return processedMessageIds.add(messageId);
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        InboxStore inbox = new InboxStore(new HashSet<>());

        // First delivery: successfully inserted and processed
        boolean firstProcessed = inbox.processMessage("msg-100"); // true

        // Duplicate redelivery: rejected by unique primary key
        boolean duplicateProcessed = inbox.processMessage("msg-100"); // false

        System.out.println(
                "First delivery admitted: "
                        + firstProcessed
                        + ", Duplicate delivery deduplicated: "
                        + !duplicateProcessed);
    }
}
