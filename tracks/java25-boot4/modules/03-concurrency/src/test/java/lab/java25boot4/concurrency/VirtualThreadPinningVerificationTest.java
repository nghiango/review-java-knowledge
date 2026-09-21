package lab.java25boot4.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VirtualThreadPinningVerificationTest {

    private final VirtualThreadPinningVerification verification =
            new VirtualThreadPinningVerification();

    @Test
    @DisplayName(
            "Should verify virtual thread unmounts cleanly during synchronized blocking operation")
    void executeSynchronizedBlockingOperation_completesSuccessfully() throws Exception {
        boolean completed = verification.executeSynchronizedBlockingOperation();
        assertThat(completed).isTrue();
    }
}
