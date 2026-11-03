## Generics and PECS

Generics allow classes, interfaces, and methods to operate on different types while maintaining compile-time type safety.

A **generic class** declares one or more type parameters.

```java

public class Box<T> {

    private T value;

    public Box(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
```

**Usage**

```java
Box<String> name = new Box<>("Alice");
Box<Integer> age = new Box<>(30);

String result = name.getValue();
Integer number = age.getValue();
```

Here, T is a type parameter, while String and Integer are type arguments.

#### Common naming conventions

| Parameter | Meaning |
|-----------|---------|
| T | Type |
| E | Element |
| K | Key |
| V | Value |
| R | Result |
| N | Number |

These are conventions, not Java keywords.

A **generic method** declares its own type parameters independently of the class.

```java
public class Utils {

    public static <T> T first(List<T> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                "List must not be empty"
            );
        }

        return items.get(0);
    }
}
```

**Usage**

```java
String name = Utils.first(List.of("Alice", "Bob"));
Integer number = Utils.first(List.of(10, 20));
```

The <T> before the return type declares the method's type parameter.

### Bounded type parameters

A **bounded type parameter** restricts the types that can be used as arguments for a type parameter.

```java
public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) > 0 ? a : b;
}
```

The `<T extends Comparable<T>>` clause means T must implement the `Comparable<T>` interface.

**Usage**

```java
String maxString = Utils.max("apple", "banana");
Integer maxInt = Utils.max(10, 20);
```

Multiple bounds are also supported:

```java
<T extends Number & Comparable<T>>
```

### Generic types are invariant

```java
Integer extends Number

// It doesn't support like this
List<Integer> extends List<Number>

List<Integer> integers = List.of(1, 2, 3);

// Compilation error
List<Number> numbers = integers;

// If it were supported, this error would happen

List<Integer> integers = new ArrayList<>();

List<Number> numbers = integers; // Hypothetical

numbers.add(3.14); // Double

Integer value = integers.get(0);
```

### Wildcards and PECS

The wildcard ? represents an unknown type.

Three important forms are:

| Syntax | Meaning |
|--------|---------|
| `List<?>` | List of some unknown element type |
| `List<? extends Number>` | List of an unknown subtype of Number |
| `List<? super Integer>` | List of an unknown supertype of Integer |

The distinction between extends and super is critical.

#### ? extends T — Upper-bounded wildcard

Use ? extends T when a method needs to **read values as type T** from a collection whose element type may be a subtype of T.

```java
public static double sum(
        List<? extends Number> numbers) {

    double total = 0;

    for (Number number : numbers) {
        total += number.doubleValue();
    }

    return total;
}
```

This method accepts:

```java
List<Integer> integers = List.of(1, 2, 3);
List<Double> doubles = List.of(1.5, 2.5);

sum(integers);
sum(doubles);
```

**Why can't we add elements?**

```java
void process(List<? extends Number> numbers) {
    // numbers.add(10);   // Compilation error
    // numbers.add(3.14); // Compilation error
}
```

The compiler does not know the exact element type.

The actual list might be List<Integer>, List<Double>, or another subtype of Number.

Adding an arbitrary Number would not be safe.

You can add null, but not a non-null element of an arbitrary subtype.

**Rule**: ? extends T allows reading elements as T, but generally prevents adding non-null elements.

#### ? super T — Lower-bounded wildcard

Use ? super T when a collection needs to accept values of type T.

```java
public static void addNumbers(
        List<? super Integer> destination) {

    destination.add(10);
    destination.add(20);
}
```

This method accepts:

```java
List<Integer> integers = new ArrayList<>();
List<Number> numbers = new ArrayList<>();
List<Object> objects = new ArrayList<>();

addNumbers(integers);
addNumbers(numbers);
addNumbers(objects);
```

**Why can't we read an Integer directly?**

```java
void process(List<? super Integer> numbers) {
    Object value = numbers.get(0);

    // Integer value = numbers.get(0);
    // Compilation error
}
```

The actual list might be a List<Object>, containing values that are not Integers.

Therefore, the compiler only guarantees that a retrieved value can be treated as Object.

**Rule**: ? super T allows writing T values, but retrieved values are only guaranteed to be Object.

#### PECS — Producer Extends, Consumer Super

PECS is a guideline for choosing wildcard bounds.

- Producer Extends: If a collection produces values for your code to read, use ? extends T.
- Consumer Super: If a collection consumes values your code writes, use ? super T.

**Example**

```java

public static <T> void copy(
        List<? extends T> source,
        List<? super T> destination) {

    for (T item : source) {
        destination.add(item);
    }
}
```

**Usage**

```java
List<Integer> source = List.of(1, 2, 3);
List<Number> destination = new ArrayList<>();

copy(source, destination);

System.out.println(destination);
// [1, 2, 3]
```

#### Unbounded wildcard <?>

The unbounded wildcard <?> represents a list of unknown type. It is equivalent to <? extends Object>.

```java
public static void printList(List<?> list) {
    for (Object element : list) {
        System.out.println(element);
    }
}
```

**Usage**

```java
List<Integer> integers = List.of(1, 2, 3);
List<String> strings = List.of("a", "b", "c");

printList(integers);
printList(strings);
```

#### List<?> vs ListObject

They are not equivalent.

```java
List<String> strings = List.of("A");

// Valid:
List<?> unknown = strings;

// Invalid:
// List<Object> objects = strings;
```

List<Object> is specifically a list whose element type is Object.

List<?> can refer to a list of any element type.

### Type Erasure

Type erasure is a process where the Java compiler removes all type parameters and replaces them with their bounds or Object if the type parameter is unbounded. This happens during compilation, and the generic type information is not available at runtime.

**Example**

```java
List<String> strings = new ArrayList<>();
List<Integer> integers = new ArrayList<>();

System.out.println(strings.getClass() == integers.getClass());
// true
```

The above code will print `true` because the generic type information is removed during compilation.

Consider:

```java
class Box<T> {
    private T value;

    public T get() {
        return value;
    }
}
```

After erasure, the representation is conceptually similar to:

```java
class Box {
    private Object value;

    public Object get() {
        return value;
    }
}
```

When a type parameter has a bound, the compiler uses its leftmost bound instead of Object.

For example:

```java
class NumberBox<T extends Number> {
    T value;
}
```

Conceptually erases to:

```java
class NumberBox {
    Number value;
}
```

The compiler inserts the necessary casts when required.

#### Restrictions caused by type erasure

- Cannot create an instance of a type parameter

```java
class Factory<T> {
    // T value = new T(); // Compilation error
}
```

The runtime does not generally know which class T represents.

A common alternative is to pass a Supplier<T>:

```java
class Factory<T> {

    private final Supplier<T> supplier;

    Factory(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    T create() {
        return supplier.get();
    }
}

Factory<ArrayList<String>> factory =
    new Factory<>(ArrayList::new);
```

- Cannot use parameterized types with instanceof

```java
Object value = List.of("A", "B");

// Invalid:
// if (value instanceof List<String>) { }
```

**Instead**

```java
if (value instanceof List<?> list) {
    System.out.println(list.size());
}
```

- Cannot overload methods that erase to the same signature

```java
// Compilation error: name clash

void process(List<String> values) {}

void process(List<Integer> values) {}
```

Both erase to process(List).

- Cannot directly create arrays of parameterized types

```java
// Invalid:
List<String>[] lists = new List<String>[10];
```

Arrays are reified and covariant, while generics use erasure and are invariant. Mixing these mechanisms without restrictions would undermine type safety.

#### Bridge methods

The compiler may generate bridge methods to preserve polymorphism after type erasure.

Consider:

```java
class Parent<T> {
    T get() {
        return null;
    }
}

class Child extends Parent<String> {
    @Override
    String get() {
        return "Hello";
    }
}
```

After erasure, `Parent.get()` returns Object, while `Child.get()` returns String.

The compiler can generate a synthetic bridge method in Child that returns Object and delegates to the String-returning method.

This preserves overriding behavior at the bytecode level.

Interview takeaway: Type erasure can change method signatures, so the compiler uses bridge methods when necessary to preserve polymorphism.

### Common Senior Java Interview Traps

| Statement | Correct? | Explanation |
|-----------|----------|-------------|
| List<Integer> is a subtype of List<Number> | No | Generics are invariant |
| List<? extends Number> can accept a List<Integer> | Yes | Upper-bounded wildcard |
| You can add an Integer to List<? extends Number> | No | Exact subtype is unknown |
| You can add an Integer to List<? super Integer> | Yes | All permitted element types accept Integer |
| List<?> and List<Object> are equivalent | No | Unknown type vs specifically Object |
| Generic type arguments are always available at runtime | No | Type erasure removes most type arguments from object instances |
| Raw types can undermine generic type safety | Yes | They bypass normal checks |
| new T() is valid in a generic class | No | Type parameter is not directly instantiable |

One nuance worth remembering: Although most generic type arguments are erased from object instances, some generic declarations remain accessible through reflection metadata, such as generic superclass and method signatures.

### Model interview answer

??? question "Model answer"
    Java Generics provide compile-time type safety and allow classes, interfaces, and methods to work with different types without requiring explicit casts.

    For example, List of String ensures that only Strings can be added through a properly typed reference.

    Java generics are invariant, meaning List of Integer is not a subtype of List of Number, even though Integer extends Number.

    Wildcards provide flexibility. The PECS principle means Producer Extends and Consumer Super. I use ? extends T when reading values from a source, and ? super T when writing values into a destination.

    Java implements generics primarily through type erasure. The compiler checks types and removes most generic type information from the bytecode, inserting casts and generating bridge methods when necessary.

    Because of erasure, we cannot directly instantiate T, overload methods that erase to the same signature, or use instanceof with a concrete parameterized type.

    I also avoid raw types and unchecked casts because they can cause heap pollution and runtime ClassCastException.

    In backend applications, I use generics to design reusable, type-safe abstractions such as repositories, API responses, and processing pipelines.
