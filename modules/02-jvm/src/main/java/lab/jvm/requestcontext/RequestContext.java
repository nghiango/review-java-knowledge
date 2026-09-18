package lab.jvm.requestcontext;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RequestContext {
    // Instance scope is intentional: separate contexts must not share request state.
    @SuppressWarnings("ThreadLocalUsage")
    private final ThreadLocal<RequestIdentity> current = new ThreadLocal<>();

    public ContextScope open(RequestIdentity identity) {
        Objects.requireNonNull(identity, "identity must not be null");
        RequestIdentity previous = current.get();
        current.set(identity);
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                if (previous == null) {
                    current.remove();
                } else {
                    current.set(previous);
                }
            }
        };
    }

    public Optional<RequestIdentity> current() {
        return Optional.ofNullable(current.get());
    }
}
