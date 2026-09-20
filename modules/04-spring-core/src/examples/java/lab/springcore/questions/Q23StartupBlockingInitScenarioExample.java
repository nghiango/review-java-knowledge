package lab.springcore.questions;

import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Q23 Scenario: Diagnosing Spring ApplicationContext startup timeout and resolving with event
 * warmup.
 */
@SuppressWarnings("unused")
public class Q23StartupBlockingInitScenarioExample {

    static class DataPreloader {
        private boolean loaded = false;

        // FIX: Warmup executed in event listener AFTER ApplicationContext is healthy and active
        public void onApplicationRefreshed(ContextRefreshedEvent event) {
            this.loaded = true;
        }

        public boolean isLoaded() {
            return loaded;
        }
    }

    public static void main(String[] args) {
        DataPreloader preloader = new DataPreloader();
        preloader.onApplicationRefreshed(null);

        boolean ready = preloader.isLoaded(); // true
    }
}
