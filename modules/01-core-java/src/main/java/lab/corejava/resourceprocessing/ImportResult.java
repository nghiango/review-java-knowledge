package lab.corejava.resourceprocessing;

import java.util.List;

public record ImportResult(List<ImportedCustomer> accepted, List<ImportFailure> failures) {

    public ImportResult {
        accepted = List.copyOf(accepted);
        failures = List.copyOf(failures);
    }
}
