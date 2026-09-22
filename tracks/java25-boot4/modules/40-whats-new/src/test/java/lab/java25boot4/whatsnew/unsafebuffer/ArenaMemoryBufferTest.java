package lab.java25boot4.whatsnew.unsafebuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ArenaMemoryBufferTest {

    @Test
    void setThenGet_roundTripsValues() {
        try (ArenaMemoryBuffer buffer = new ArenaMemoryBuffer(4)) {
            buffer.set(0, 42L);
            buffer.set(3, -1L);

            assertThat(buffer.get(0)).isEqualTo(42L);
            assertThat(buffer.get(3)).isEqualTo(-1L);
        }
    }

    @Test
    void access_outOfBounds_throwsInsteadOfCorruptingMemory() {
        try (ArenaMemoryBuffer buffer = new ArenaMemoryBuffer(2)) {
            assertThatThrownBy(() -> buffer.set(2, 1L)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> buffer.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Test
    void close_releasesNativeMemory() {
        ArenaMemoryBuffer buffer = new ArenaMemoryBuffer(1);
        buffer.set(0, 7L);

        buffer.close();

        assertThatThrownBy(() -> buffer.get(0)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_nonPositiveCapacity_isRejected() {
        assertThatThrownBy(() -> new ArenaMemoryBuffer(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity");
    }
}
