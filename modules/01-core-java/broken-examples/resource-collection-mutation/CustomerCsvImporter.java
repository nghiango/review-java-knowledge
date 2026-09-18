package lab.corejava.broken.resourceprocessing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CustomerCsvImporter {

    public record Customer(String customerId, String email) {}

    public void importInto(Path path, List<Customer> customers) throws IOException {
        var reader = Files.newBufferedReader(path);
        String row;
        while ((row = reader.readLine()) != null) {
            var fields = row.split(",");
            customers.add(new Customer(fields[0], fields[1]));
        }

        for (var customer : customers) {
            if (customer.email().isBlank()) {
                customers.remove(customer);
            }
        }
    }
}
