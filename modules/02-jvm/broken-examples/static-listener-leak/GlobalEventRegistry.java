package lab.jvm.broken.staticlistenerleak;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class GlobalEventRegistry {
    private static final List<Consumer<String>> LISTENERS = new ArrayList<>();

    private GlobalEventRegistry() {}

    public static void register(Consumer<String> listener) {
        LISTENERS.add(listener);
    }

    public static List<Consumer<String>> listeners() {
        return LISTENERS;
    }

    public static void publish(String event) {
        for (Consumer<String> listener : LISTENERS) {
            listener.accept(event);
        }
    }
}
