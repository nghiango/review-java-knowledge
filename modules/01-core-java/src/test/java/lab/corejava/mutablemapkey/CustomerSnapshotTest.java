package lab.corejava.mutablemapkey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerSnapshotTest {

    @Test
    void constructor_mutableTagsAreDefensivelyCopied() {
        var tags = new ArrayList<>(List.of("priority"));
        var snapshot =
                new CustomerSnapshot(new CustomerKey("tenant-a", "customer-42"), "Ada", tags);

        tags.add("overdue");

        assertThat(snapshot.tags()).containsExactly("priority");
        assertThatThrownBy(() -> snapshot.tags().add("blocked"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_nullRequiredValuesAreRejected() {
        var key = new CustomerKey("tenant-a", "customer-42");

        assertThatNullPointerException()
                .isThrownBy(() -> new CustomerSnapshot(null, "Ada", List.of()))
                .withMessage("key");
        assertThatNullPointerException()
                .isThrownBy(() -> new CustomerSnapshot(key, null, List.of()))
                .withMessage("displayName");
        assertThatNullPointerException()
                .isThrownBy(() -> new CustomerSnapshot(key, "Ada", null))
                .withMessage("tags");
    }
}
