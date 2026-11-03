## String, String Builders, and String Buffer

All three classes represent sequences of characters, but they differ in mutability, thread safety, and performance.

User `String` by default. Use `StringBuilder` for single-threaded mutable string building. Use `StringBuffer` only when its synchronized, thread safety is required.

### String - Immutable

A String object cannot be modified after creation

Any operation that appears to change a String produces another String value rather than modifying the existing object.

![String memory visualization](images/string-memory-visualization.png)

#### Why is String immutable?

- Security: String values cannot be changed through and existing reference after creation.
- Thread safety: Immutable String objects can safely be shared between threads without synchronization.
- String pooling: Identical literals can share the same object.
- Hasing: A String's hash code remains stable, making it suitable as a HashMap key.

### StringBuilder - Mutable, not thread-safe

`StringBuilder` maintains a mutable character sequence.

Instead of creating a new String for each modification, it generally modifies its internal buffer, expanding capacity when necessary.

`StringBuilder` is generally the preferred option for building strings inside loops or when many modifications are needed. All the operations on `StringBuilder` are not thread-safe.

### StringBuffer - Mutable, thread-safe

`StringBuffer` is similar to `StringBuilder` but is thread-safe. All the operations on `StringBuffer` are thread-safe, which means it is slower than `StringBuilder` but can be used in multi-threaded environments.

!!! note "Consider using"
    Although `StringBuilder` and `StringBuffer` are good for concatenating strings, which doesn't mean `String` concatenation is always slow. For simple concatenation expressions, modern Java compilers and the JVM can optimize string concatenation efficiently. Use `StringBuilder` when repeated accumulation make it useful, not automatically for every `+` operation.

### Model interview answer

??? question "Model answer"
    String, StringBuilder, and StringBuffer all represent sequences of characters, but they differ in mutability and thread safety.

    String is immutable, so once a String object is created, its content cannot change. Any modification produces a new String value. This makes it safe to share across threads and useful as a HashMap key.

    StringBuilder is mutable and not thread-safe. It allows efficient modifications through methods like append, insert, and delete. I normally use it when constructing dynamic strings, especially inside loops.

    StringBuffer is also mutable, but its methods are synchronized, which provides thread safety for individual operations. This synchronization can introduce additional overhead.

    In practice, I use String by default, StringBuilder for repeated modifications in a single-threaded context, and StringBuffer only when I specifically need synchronized access to a shared mutable character sequence.

    I also understand that modern Java optimizes simple String concatenation, so using StringBuilder is not always necessary.
