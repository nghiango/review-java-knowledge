package lab.corejava.resourceprocessing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CustomerCsvImporter {

    public ImportResult importFrom(CustomerCsvSource source) throws IOException {
        Objects.requireNonNull(source, "source");
        var accepted = new ArrayList<ImportedCustomer>();
        var failures = new ArrayList<ImportFailure>();

        try (var reader = source.open()) {
            String row;
            var lineNumber = 0;
            while ((row = reader.readLine()) != null) {
                lineNumber++;
                parse(row, lineNumber, accepted, failures);
            }
        }

        return new ImportResult(accepted, failures);
    }

    private static void parse(
            String row,
            int lineNumber,
            List<ImportedCustomer> accepted,
            List<ImportFailure> failures) {
        var fields = row.split(",", -1);
        if (fields.length != 2) {
            failures.add(new ImportFailure(lineNumber, row, "expected customerId,email"));
            return;
        }
        var customerId = fields[0].strip();
        var email = fields[1].strip();
        if (customerId.isBlank()) {
            failures.add(new ImportFailure(lineNumber, row, "customerId must not be blank"));
        } else if (email.isBlank()) {
            failures.add(new ImportFailure(lineNumber, row, "email must not be blank"));
        } else {
            accepted.add(new ImportedCustomer(customerId, email));
        }
    }
}
