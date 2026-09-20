# Solution: Missing Pagination & Unbounded List

## Annotated Code

### `CatalogController.java`
```java
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

    // Performance issue: Unbounded collection retrieval returns all records without pagination, causing JVM OOM and saturating DB I/O and network bandwidth.
    // API Contract issue: Returning raw List instead of a paginated envelope prevents clients from discovering page metadata (total elements, page count, next/previous cursors).
    @GetMapping
    public List<CatalogItem> getAllItems() {
        return database;
    }

    // Security issue: Unbounded or excessively high limit parameter allows clients to trigger denial-of-service via resource exhaustion (e.g. limit=1000000).
    // Performance issue: Offset-based pagination with in-memory stream skipping scales linearly O(N), degrading severely with deep pages; cursor/keyset pagination should be used for large datasets.
    @GetMapping("/search")
    public List<CatalogItem> searchItems(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "1000") int limit) {
        return database.stream()
                .filter(item -> category == null || item.category().equalsIgnoreCase(category))
                .skip(offset)
                .limit(limit)
                .toList();
    }
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| Unbounded `getAllItems` endpoint | Performance | Critical | Querying and returning the entire database collection in memory leads to Heap exhaustion (`OutOfMemoryError`), database connection starvation, and extreme JSON serialization latency. |
| Missing pagination envelope / cursor | API Contract | Major | Returning a bare array `List<T>` provides no pagination metadata (e.g., `page`, `size`, `totalElements`, `hasNext`, cursor tokens), preventing consumers from implementing reliable pagination or navigation. |
| Uncapped `limit` parameter | Security | Major | Allowing unconstrained limit sizes (or default 1000) exposes the API to memory and CPU resource exhaustion attacks (CWE-400 / Denial of Service). Page size must be capped by a strict upper bound (e.g. max 100). |
| Deep offset performance degradation | Performance | Major | In-memory or SQL offset pagination requires scanning and discarding `offset` rows before returning `limit` rows ($O(\text{offset} + \text{limit})$). High offsets cause high query latency and cache thrashing. Keyset (cursor-based) pagination (`id > lastSeenId`) is preferred. |

## Correct Implementation Reference
- [`PagedResponse.java`](../../src/main/java/lab/restapi/pagination/PagedResponse.java)
- [`CatalogItemRepository.java`](../../src/main/java/lab/restapi/pagination/CatalogItemRepository.java)
- [`SafeCatalogController.java`](../../src/main/java/lab/restapi/pagination/SafeCatalogController.java)
