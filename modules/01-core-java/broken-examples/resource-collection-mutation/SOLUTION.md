# Solution: Customer CSV import

## Annotated code

```java
public void importInto(Path path, List<Customer> customers) throws IOException {
    // Resource leak issue: The importer owns this reader but never closes it on success or failure.
    var reader = Files.newBufferedReader(path);
    String row;
    while ((row = reader.readLine()) != null) {
        var fields = row.split(",");
        // Data consistency issue: Caller-owned state is changed row by row. A malformed later row
        // leaves a partial import with no result describing what was committed.
        customers.add(new Customer(fields[0], fields[1]));
    }

    // Reliability issue: Removing structurally through the collection while its enhanced-for
    // iterator is active is fail-fast and can throw ConcurrentModificationException.
    for (var customer : customers) {
        if (customer.email().isBlank()) {
            customers.remove(customer);
        }
    }
    // Design issue: I/O, parsing, validation and mutation share one operation, so ownership and
    // partial-failure policy cannot be understood or tested independently.
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Resource leak issue | High | reader creation | Owned reader is never closed |
| 2 | Data consistency issue | High | row loop | Caller list is partially mutated on failure |
| 3 | Reliability issue | High | validation loop | Structural mutation invalidates iterator |
| 4 | Design issue | Medium | `importInto()` | I/O, parsing, validation and mutation are coupled |

## Issue details

### Reader is never closed

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Basic

The method creates and therefore owns the reader, but no success or exceptional path closes it.
Repeated imports exhaust file descriptors. `CustomerCsvImporter.importFrom` uses try-with-resources,
which also preserves a close failure as suppressed when reading already failed.

**Trade-off:** Explicit ownership means a caller that needs reuse must supply a different source
abstraction rather than retaining the reader.

**Detection:** Monitor open file descriptors (`lsof`, process metrics) and inspect allocation sites.

### Partial mutation on malformed input

**Type:** Data consistency issue · **Severity:** High · **Difficulty:** Intermediate

Rows are appended directly to caller state before the whole input is classified. A malformed row
leaves earlier rows installed. The correct importer builds local accepted/failure lists and returns
one immutable `ImportResult` for the caller to apply deliberately.

**Trade-off:** Buffering the result consumes memory proportional to the import; very large files may
need transactional chunks and checkpointing instead.

**Detection:** Inject a malformed row after valid rows and inspect caller state after failure.

### Iterator invalidated by structural removal

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic

Enhanced `for` uses an Iterator whose modification count detects removal through the collection.
Use iterator removal when in-place mutation is intentional, `removeIf`, or preferably derive a new
validated result as this implementation does.

**Trade-off:** Copying avoids side effects but allocates a result collection.

**Detection:** Include at least one invalid row after iteration begins.

### Responsibilities are coupled

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

One method owns file I/O, CSV shape, validation and mutation policy. The corrected design injects a
`CustomerCsvSource`, parses into values and returns an explicit result without mutating its caller.

**Trade-off:** More small types make ownership and failure semantics explicit at the cost of API
surface.

**Detection:** Tests that require filesystem setup to exercise a validation rule expose coupling.

## Correct implementation

Package: `lab.corejava.resourceprocessing`

- `src/main/java/lab/corejava/resourceprocessing/CustomerCsvImporter.java`
- `docs/topics/core-java/solutions.md#resource-safe-csv-processing`
