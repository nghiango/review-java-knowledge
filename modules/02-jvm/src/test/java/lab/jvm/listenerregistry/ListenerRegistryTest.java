package lab.jvm.listenerregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ListenerRegistryTest {
    private final ListenerRegistry<String> registry = new ListenerRegistry<>();

    @Test
    void snapshot_whenListenerRegistered_containsListenerAndCannotBeMutated() {
        registry.register("first-listener");

        assertThat(registry.snapshot()).containsExactly("first-listener");
        assertThatThrownBy(() -> registry.snapshot().add("second-listener"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void close_whenRegistrationClosed_removesOnlyThatListener() {
        Registration first = registry.register("first-listener");
        registry.register("second-listener");

        first.close();

        assertThat(registry.snapshot()).containsExactly("second-listener");
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void close_whenCalledRepeatedly_isHarmless() {
        Registration registration = registry.register("listener");

        registration.close();
        registration.close();

        assertThat(registry.snapshot()).isEmpty();
        assertThat(registry.size()).isZero();
    }

    @Test
    void snapshot_whenTwoRegistryInstancesUsed_keepsStateIsolated() {
        ListenerRegistry<String> otherRegistry = new ListenerRegistry<>();

        registry.register("first-listener");
        otherRegistry.register("second-listener");

        assertThat(registry.snapshot()).containsExactly("first-listener");
        assertThat(otherRegistry.snapshot()).containsExactly("second-listener");
    }

    @Test
    void register_whenListenerIsNull_rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> registry.register(null));
    }
}
