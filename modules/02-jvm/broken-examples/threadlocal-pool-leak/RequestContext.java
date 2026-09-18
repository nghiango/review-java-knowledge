package lab.jvm.broken.threadlocalpoolleak;

import java.util.Optional;

public final class RequestContext {
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    private RequestContext() {}

    public static void setUser(String userId) {
        CURRENT_USER.set(userId);
    }

    public static Optional<String> currentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }
}
