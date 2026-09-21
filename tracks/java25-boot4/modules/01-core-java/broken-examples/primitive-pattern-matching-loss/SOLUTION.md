# Solution: Primitive Pattern Matching & Lossy Casts

## Annotated Code

```java
package lab.java25boot4.corejava.broken.patterns;

public class MetricConverter {

    public String formatMetric(Object value) {
        // Design issue: Primitive pattern order hazard; instanceof byte matches Byte, but Integer values will fall through or fail unexpectedly
        if (value instanceof byte b) {
            return "byte: " + b;
        } else if (value instanceof int i) {
            return "int: " + i;
        } else if (value instanceof long l) {
            return "long: " + l;
        } else if (value instanceof double d) {
            return "double: " + d;
        }
        return "unknown: " + value;
    }

    public int castToSmallInt(Object value) {
        if (value instanceof int i) {
            // Reliability issue: Unchecked narrowing primitive cast causes silent overflow/truncation for integers exceeding Byte.MAX_VALUE (127)
            return (byte) i;
        }
        return -1;
    }
}
```

## Issue Analysis

### 1. Unchecked Primitive Narrowing & Truncation (Reliability)
- **Problem**: In Java 25, primitive types can be matched in patterns. However, narrowing conversions (`int` to `byte`) wrap silently without bounds checking. For example, if `value` is `300`, `(byte) 300` evaluates to `44`, corrupting business metrics without any warning or exception.
- **Correction**: Guard the pattern with an explicit range condition (`when i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE`), or use safe conversion methods like `Math.toIntExact`.

### 2. Ambiguity in Primitive Boxing with Object Targets (Design)
- **Problem**: When `value` is typed as `Object`, primitive patterns unbox wrapper types (`Integer`, `Long`, `Byte`). Relying on cascade `if/else` checks can produce surprising evaluations if numeric wrappers of different precisions are mixed.
- **Correction**: Use modern Java 25 `switch` expressions with exhaustive branches and guarded pattern clauses (`when`).

---

## Correct Implementation

```java
package lab.java25boot4.corejava;

public class SafeMetricConverter {

    public String formatMetric(Object value) {
        return switch (value) {
            case Byte b -> "byte: " + b;
            case Short s -> "short: " + s;
            case Integer i -> "int: " + i;
            case Long l -> "long: " + l;
            case Double d -> "double: " + d;
            case null -> "null";
            default -> "unknown: " + value;
        };
    }

    public int castToSmallInt(Object value) {
        return switch (value) {
            case Integer i when i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE -> i;
            case Integer _ -> throw new IllegalArgumentException("Integer exceeds byte capacity");
            default -> -1;
        };
    }
}
```
