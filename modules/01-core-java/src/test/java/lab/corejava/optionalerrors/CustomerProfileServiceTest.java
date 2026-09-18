package lab.corejava.optionalerrors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerProfileServiceTest {

    @Test
    void requireProfile_existingNormalizedEmail_returnsProfile() {
        var profile = new CustomerProfile("customer-42", "ada@example.com", "Ada");
        var repository = new RecordingRepository(Optional.of(profile));
        var service = new CustomerProfileService(repository);

        assertThat(service.requireProfile("  ADA@EXAMPLE.COM ")).isSameAs(profile);
        assertThat(repository.lastEmail).isEqualTo("ada@example.com");
    }

    @Test
    void requireProfile_missingProfile_throwsContextualDomainException() {
        var service = new CustomerProfileService(new RecordingRepository(Optional.empty()));

        assertThatThrownBy(() -> service.requireProfile("missing@example.com"))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("missing@example.com")
                .extracting("email")
                .isEqualTo("missing@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void requireProfile_blankEmail_rejectsBeforeRepositoryCall(String email) {
        var repository = new RecordingRepository(Optional.empty());
        var service = new CustomerProfileService(repository);

        assertThatThrownBy(() -> service.requireProfile(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
        assertThat(repository.calls).isZero();
    }

    @Test
    void requireProfile_repositoryFailure_propagatesUnchanged() {
        var failure = new IllegalStateException("database unavailable");
        CustomerProfileRepository repository =
                email -> {
                    throw failure;
                };
        var service = new CustomerProfileService(repository);

        assertThatThrownBy(() -> service.requireProfile("ada@example.com")).isSameAs(failure);
    }

    private static final class RecordingRepository implements CustomerProfileRepository {
        private final Optional<CustomerProfile> result;
        private int calls;
        private String lastEmail;

        private RecordingRepository(Optional<CustomerProfile> result) {
            this.result = result;
        }

        @Override
        public Optional<CustomerProfile> findByEmail(String email) {
            calls++;
            lastEmail = email;
            return result;
        }
    }
}
