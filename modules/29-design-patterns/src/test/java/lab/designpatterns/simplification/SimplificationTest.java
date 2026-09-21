package lab.designpatterns.simplification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimplificationTest {

    @Test
    @DisplayName(
            "Direct CSV export correctly serializes user records with zero pattern over-engineering")
    void toCsv_validUsers_producesExpectedCsv() {
        List<UserRecord> users =
                List.of(
                        new UserRecord("u-1", "john_doe", "john@example.com"),
                        new UserRecord("u-2", "jane_smith", "jane@example.com"));

        String csv = UserCsvExporter.toCsv(users);

        assertThat(csv)
                .isEqualTo(
                        """
                id,username,email
                u-1,john_doe,john@example.com
                u-2,jane_smith,jane@example.com
                """);
    }
}
