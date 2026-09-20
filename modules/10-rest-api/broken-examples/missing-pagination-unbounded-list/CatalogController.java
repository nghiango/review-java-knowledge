package lab.restapi.broken.unboundedlist;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final List<CatalogItem> database = new ArrayList<>();

    @GetMapping
    public List<CatalogItem> getAllItems() {
        // Loads entire table into JVM memory and serializes all records
        return database;
    }

    @GetMapping("/search")
    public List<CatalogItem> searchItems(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "1000") int limit) {
        // Uncapped limit allows client to request millions of rows; deep offset causes linear scan
        return database.stream()
                .filter(item -> category == null || item.category().equalsIgnoreCase(category))
                .skip(offset)
                .limit(limit)
                .toList();
    }
}
