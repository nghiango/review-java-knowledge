package lab.springcore.questions;

import org.springframework.beans.factory.ObjectProvider;

@SuppressWarnings("unused")
public final class Q29PrototypeInSingletonLeakScenarioExample {
    private Q29PrototypeInSingletonLeakScenarioExample() {}

    // Prototype bean intended to carry mutable per-request state
    public static class RequestContextHolder {
        private String userId;

        public void setUserId(String userId) { this.userId = userId; }
        public String getUserId() { return userId; }
    }

    // Problematic Singleton: Direct field injection caches a SINGLE prototype instance forever!
    public static class FlawedSingletonService {
        private final RequestContextHolder holder; // Sits in singleton memory, shared across all threads!

        public FlawedSingletonService(RequestContextHolder holder) {
            this.holder = holder;
        }

        public void handleRequest(String user) {
            holder.setUserId(user); // Race condition! User A overwrites User B's state!
        }
    }

    // Proper Solution: ObjectProvider lazily resolves a fresh prototype instance per invocation
    public static class SafeSingletonService {
        private final ObjectProvider<RequestContextHolder> holderProvider;

        public SafeSingletonService(ObjectProvider<RequestContextHolder> holderProvider) {
            this.holderProvider = holderProvider;
        }

        public String handleRequest(String user) {
            RequestContextHolder fresh = holderProvider.getObject(); // Fresh instance created each call
            fresh.setUserId(user);
            return fresh.getUserId();
        }
    }

    public static void main(String[] args) {
        ObjectProvider<RequestContextHolder> mockProvider = new ObjectProvider<>() {
            @Override
            public RequestContextHolder getObject(Object... args) { return new RequestContextHolder(); }
            @Override
            public RequestContextHolder getIfAvailable() { return new RequestContextHolder(); }
            @Override
            public RequestContextHolder getIfUnique() { return new RequestContextHolder(); }
            @Override
            public RequestContextHolder getObject() { return new RequestContextHolder(); }
        };

        SafeSingletonService safeService = new SafeSingletonService(mockProvider);
        String user = safeService.handleRequest("user-99"); // "user-99"
    }
}
