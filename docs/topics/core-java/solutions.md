# Core Java Solutions

## Issue-to-fix matrix

| Broken contract | Correct design | Main trade-off |
|---|---|---|
| Mutable/inconsistent map identity | Validated final `CustomerKey` record; profile snapshot separated | Copies and explicit identity migration |
| Optional and broad catches everywhere | Stateless boundary validates input and maps only expected absence | More explicit domain error contract |
| Leaked reader and partial caller mutation | Try-with-resources plus immutable `ImportResult` | Result buffering; large imports need chunks |
| Shared parallel-stream side effects | Pure mapping plus caller-owned bounded executor | Executor sizing, monitoring and shutdown ownership |

## Immutable map identity

=== "Broken"

    ```java
    --8<-- "modules/01-core-java/broken-examples/mutable-map-key/CustomerKey.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/01-core-java/src/main/java/lab/corejava/mutablemapkey/CustomerKey.java"
    ```

A final record keeps equality/hash state stable and compatible. Profile display data and copied tags
live in `CustomerSnapshot`. Copying is O(n), and identity changes require explicit migration.

## Explicit absence and failure

=== "Broken"

    ```java
    --8<-- "modules/01-core-java/broken-examples/optional-exception-misuse/CustomerProfileService.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/01-core-java/src/main/java/lab/corejava/optionalerrors/CustomerProfileService.java"
    ```

The boundary rejects invalid input, normalizes once, maps only expected absence to a contextual
domain exception and preserves repository failures. The service has no cross-request state.

## Resource-safe CSV processing

=== "Broken"

    ```java
    --8<-- "modules/01-core-java/broken-examples/resource-collection-mutation/CustomerCsvImporter.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/01-core-java/src/main/java/lab/corejava/resourceprocessing/CustomerCsvImporter.java"
    ```

Try-with-resources makes ownership deterministic. Parsing builds immutable accepted/failure lists
instead of mutating caller state. Large files may need chunking because this design buffers results.

## Deterministic order reporting

=== "Broken"

    ```java
    --8<-- "modules/01-core-java/broken-examples/stream-parallel-side-effects/OrderReportService.java"
    ```

=== "Correct"

    ```java
    --8<-- "modules/01-core-java/src/main/java/lab/corejava/streamprocessing/OrderReportService.java"
    ```

The sequential path is pure. The concurrent path uses a caller-owned executor and joins futures in
input order; callers must size, monitor and close that executor. Contextual exceptions retain the
failed order and cause.

## Related

- [Code review](code-review.md)
- [Tests](tests.md)
- [Production](production.md)
