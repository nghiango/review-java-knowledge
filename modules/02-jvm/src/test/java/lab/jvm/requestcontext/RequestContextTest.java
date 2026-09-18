package lab.jvm.requestcontext;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class RequestContextTest {
    private final RequestContext context = new RequestContext();
    private final RequestIdentity identity = new RequestIdentity("request-1", "user-1");

    @Test
    void current_whenScopeIsOpen_returnsIdentity() {
        try (ContextScope ignored = context.open(identity)) {
            assertThat(context.current()).contains(identity);
        }
    }

    @Test
    void current_whenScopeIsClosed_clearsIdentity() {
        ContextScope scope = context.open(identity);

        scope.close();

        assertThat(context.current()).isEmpty();
    }

    @Test
    void close_whenNestedScopeClosed_restoresParentIdentity() {
        RequestIdentity parent = new RequestIdentity("request-parent", "user-parent");
        RequestIdentity child = new RequestIdentity("request-child", "user-child");

        try (ContextScope parentScope = context.open(parent)) {
            try (ContextScope childScope = context.open(child)) {
                assertThat(context.current()).contains(child);
            }

            assertThat(context.current()).contains(parent);
        }

        assertThat(context.current()).isEmpty();
    }

    @Test
    void close_whenCalledRepeatedly_isHarmless() {
        ContextScope scope = context.open(identity);

        scope.close();
        scope.close();

        assertThat(context.current()).isEmpty();
    }

    @Test
    void current_whenExceptionLeavesTryWithResources_clearsIdentity() {
        try {
            try (ContextScope ignored = context.open(identity)) {
                throw new IllegalStateException("boom");
            }
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("boom");
        }

        assertThat(context.current()).isEmpty();
    }

    @Test
    void current_whenSingleThreadExecutorReusesThread_doesNotLeakIdentity()
            throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Optional<RequestIdentity> first =
                    executor.submit(
                                    () -> {
                                        try (ContextScope ignored = context.open(identity)) {
                                            return context.current();
                                        }
                                    })
                            .get();
            Optional<RequestIdentity> second = executor.submit(context::current).get();

            assertThat(first).contains(identity);
            assertThat(second).isEmpty();
        } finally {
            executor.shutdownNow();
        }
    }
}
