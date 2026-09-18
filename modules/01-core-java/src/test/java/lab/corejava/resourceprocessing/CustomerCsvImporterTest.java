package lab.corejava.resourceprocessing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class CustomerCsvImporterTest {

    @Test
    void importFrom_validAndInvalidRows_returnsImmutablePartitionedResultAndClosesReader()
            throws IOException {
        var closed = new AtomicBoolean();
        var reader =
                trackingReader(
                        "customer-1,ada@example.com\ninvalid\ncustomer-2, \ncustomer-3,lin@example.com",
                        closed);
        var importer = new CustomerCsvImporter();

        var result = importer.importFrom(() -> reader);

        assertThat(result.accepted())
                .containsExactly(
                        new ImportedCustomer("customer-1", "ada@example.com"),
                        new ImportedCustomer("customer-3", "lin@example.com"));
        assertThat(result.failures())
                .containsExactly(
                        new ImportFailure(2, "invalid", "expected customerId,email"),
                        new ImportFailure(3, "customer-2, ", "email must not be blank"));
        assertThat(closed).isTrue();
        assertThatThrownBy(() -> result.accepted().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.failures().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void importFrom_readFailure_closesReaderAndPropagatesCause() {
        var closed = new AtomicBoolean();
        var failure = new IOException("disk disconnected");
        var reader =
                new BufferedReader(new StringReader("unused")) {
                    @Override
                    public String readLine() throws IOException {
                        throw failure;
                    }

                    @Override
                    public void close() throws IOException {
                        closed.set(true);
                        super.close();
                    }
                };
        var importer = new CustomerCsvImporter();

        assertThatThrownBy(() -> importer.importFrom(() -> reader)).isSameAs(failure);
        assertThat(closed).isTrue();
    }

    private static BufferedReader trackingReader(String content, AtomicBoolean closed) {
        return new BufferedReader(new StringReader(content)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
    }
}
