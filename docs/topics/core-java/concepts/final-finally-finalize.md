## final, finally, and finalize

The most important distinction is that `final` is a modifier, `finally` is a part of exception handling, and `finalize` is a deprecated method.

### final - Restrict modification

In Java, `final` can be applied to variables, methods, and classes.

- **Variables**: a `final` variable can only be assigned once. After that, its value/reference cannot be changed.
- **Methods**: a `final` method cannot be overridden by any subclasses.
- **Classes**: a `final` class cannot be extended by any subclasses.

### finally - Exception handling and cleanup

The `finally` block is used with `try` and optionally `catch` blocks.

It is executed regardless of whether an exception is thrown or not.

![Try-Catch-Finally](images/try-catch-finally.png)

!!! info "Note"
    Should not return a value from a finally block, as it can mask exceptions from the try block, which can override the return value of the try block.

    For morden Java, I recommend using try-with-resources instead of try-finally for resource management.

### finalize - Object cleanup (deprecated)

`finalize` is a method that is called by the garbage collector before an object is destroyed. It is used to perform cleanup operations.

Since `finalize` is deprecated, I recommend using try-with-resources instead of relying on `finalize` for resource management.

To make the try-with-resources work, the resource must implement the `AutoCloseable` interface.

### Model interview answer

??? question "Model answer"
    In Java, final, finally, and finalize serve three different purposes.

    First, final is a modifier. When applied to a variable, it prevents reassignment. When applied to a method, it prevents overriding. When applied to a class, it prevents inheritance. However, a final reference does not necessarily make the referenced object immutable.

    Second, finally is used in exception handling. It normally executes after try and catch, even when an exception occurs or the method returns. It is useful for cleanup, although try-with-resources is preferred for managing AutoCloseable resources.

    Finally, finalize is a legacy method associated with garbage collection. It was designed for cleanup before an object is reclaimed, but its execution is not guaranteed. Since it is deprecated for removal, I would avoid it in modern Java and use AutoCloseable or Cleaner when appropriate.

    The key difference is that final restricts changes, finally controls cleanup execution, and finalize is an obsolete object cleanup mechanism.
