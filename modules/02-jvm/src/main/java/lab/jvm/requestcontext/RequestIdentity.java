package lab.jvm.requestcontext;

import java.util.Objects;

public record RequestIdentity(String requestId, String userId) {
    public RequestIdentity {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
    }
}
