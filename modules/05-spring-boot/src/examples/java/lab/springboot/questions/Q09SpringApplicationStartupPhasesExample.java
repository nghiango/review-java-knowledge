package lab.springboot.questions;

import java.util.ArrayList;
import java.util.List;

public class Q09SpringApplicationStartupPhasesExample {

    public static void main(String[] args) {
        // SpringApplication lifecycle event order
        List<String> eventSequence = new ArrayList<>();

        eventSequence.add("ApplicationStartingEvent");
        eventSequence.add("ApplicationEnvironmentPreparedEvent");
        eventSequence.add("ApplicationContextInitializedEvent");
        eventSequence.add("ApplicationPreparedEvent");
        eventSequence.add("ApplicationStartedEvent");
        eventSequence.add("ApplicationReadyEvent");

        String firstEvent = eventSequence.get(0); // "ApplicationStartingEvent"
        String finalEvent = eventSequence.get(eventSequence.size() - 1); // "ApplicationReadyEvent"
        int totalPhases = eventSequence.size(); // 6

        System.out.println(
                "First event: "
                        + firstEvent
                        + ", final event: "
                        + finalEvent
                        + ", total phases: "
                        + totalPhases);
    }
}
