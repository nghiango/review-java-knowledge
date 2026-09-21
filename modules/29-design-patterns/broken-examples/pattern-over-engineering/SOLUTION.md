# Solution: Pattern Over-Engineering

## Annotated code

```java
package lab.designpatterns.broken.overengineering;

import java.util.List;

// Maintainability issue: Over-engineered abstraction layer combining Bridge, Factory, and Visitor for a trivial string join
public class UserExportService {

    // Maintainability issue: Anonymous implementation of synthetic bridge interface obscuring simple string formatting
    private final AbstractExportFactoryProviderBridge exportBridge =
            new AbstractExportFactoryProviderBridge() {
                @Override
                public String generatePayload(ExportContextStrategyBuilder context) {
                    // Maintainability issue: Visitor pattern introduced prematurely without varying element hierarchies
                    AbstractExportFactoryProviderBridge.ExportBridgeVisitor visitor =
                            new AbstractExportFactoryProviderBridge.DefaultExportVisitorBridge();

                    StringBuilder sb = new StringBuilder("id,username,email\n");
                    for (UserRecord r : context.getRecords()) {
                        sb.append(visitor.visitRecord(r)).append("\n");
                    }
                    return sb.toString();
                }
            };

    // Maintainability issue: Unnecessary Builder and Context object allocation for a single list argument
    public String exportUsersToCsv(List<UserRecord> users) {
        ExportContextStrategyBuilder context = ExportContextStrategyBuilder.create().withRecords(users);
        return exportBridge.generatePayload(context);
    }
}
```

## Issue analysis

### 1. Premature Abstraction & YAGNI Violation (Maintainability)
- **Problem**: The author used the Gang of Four catalog as a checklist rather than solving a problem. Exporting 3 fields from a record list into CSV required 4 classes, 2 nested interfaces, a synthetic visitor, and a custom builder.
- **Consequence**: High cognitive load for future maintainers. Debugging requires stepping through 5 stack frames of delegating wrappers to format a single line of text.

### 2. Accidental Allocation & GC Pressure (Maintainability / Performance)
- **Problem**: Every export invocation allocates an `ExportContextStrategyBuilder`, an inner `ExportBridgeVisitor`, and intermediate builder contexts, adding unnecessary garbage collection pressure without any architectural benefit.

---

## Correct implementation

The production solution embraces **KISS (Keep It Simple, Stupid)** and idiomatic Java 21:
- Package: `lab.designpatterns.simplification`
- Implementation in `UserCsvExporter.java`:
  ```java
  public class UserCsvExporter {
      public static String toCsv(List<UserRecord> users) {
          StringBuilder sb = new StringBuilder("id,username,email\n");
          for (UserRecord u : users) {
              sb.append(u.id()).append(',')
                .append(u.username()).append(',')
                .append(u.email()).append('\n');
          }
          return sb.toString();
      }
  }
  ```
- Clear, readable, zero indirection, 10x faster, and trivial to test.

---

## Trade-offs

| Approach | Over-Engineered Bridge/Visitor | Idiomatic Direct Implementation |
|---|---|---|
| **Lines of Code** | 80+ lines across 4 files | ~15 lines in 1 utility class |
| **Cognitive Load** | High: multiple design patterns chained | Low: immediately obvious intent |
| **Extensibility** | Theoretical: future formats might plug in | Practical: add simple JSON/XML methods when needed |
