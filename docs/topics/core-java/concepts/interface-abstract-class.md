## Interface and Abstract Class

Both interfaces and abstract classes support abstraction, but they serve different design purposes.

Java allows to implement multiple interfaces in a class, but only extends one abstract class.

### Interface

An interface defines a contract that implementing classes must follow. It is useful when multiple classes need to provide the same behavior but may have different implementations.

#### Java 8+ Features

##### Default Methods

Interfaces can provide a default implementation for methods. This is especially useful when evoling an existing interface without forcing every implementation to immediately implement a new method.

When two interfaces provide default methods with the same signature, the implementing class must override the method to resolve the conflict.

##### Static Methods

Interfaces can provide static methods, which are utility methods that belong to the interface itself rather than to any instance.

#### Private Methods - Java 9+

Interfaces can provide private methods, which are helper methods that can be used by other methods in the interface but are not part of the public contract.

### Abstract Class

An abstract class provides a common base for related classes. It can contain instance fields, constructors, concrete methods, and abstract methods.

**Design Principle**: Prefer interfaces for contracts, and prefer composition over inheritance when code reuse does not represent a genuine "is-a" relationship.

An interface is not automatically better than an abstract class. The choice depends on the relationship between the types and the responsibilities they share.

### Model interview answer

??? question "Model answer"
    Both interfaces and abstract classes support abstraction in Java, but they have different purposes.

    An interface defines a contract that implementing classes must follow. It is useful for loose coupling and polymorphism, especially when unrelated classes need to provide the same capability. A class can implement multiple interfaces.

    An abstract class provides a common base for related subclasses. It can contain instance fields, constructors, concrete methods, and abstract methods. However, Java only allows a class to extend one superclass.

    Since Java 8, interfaces can also have default and static methods, and since Java 9, they can have private methods. However, interfaces still cannot maintain per-instance state like abstract classes.

    In practice, I prefer interfaces for defining contracts, such as PaymentProcessor, and abstract classes when closely related subclasses share meaningful state or implementation, such as different types of BankAccount.

    I also consider composition when the goal is simply to reuse behavior without creating a strong inheritance relationship.

### Key Differences

- **Inheritance**: A class can implement multiple interfaces but can only extend one abstract class.
- **Method Implementation**: An interface can only have abstract methods (before Java 8), while an abstract class can have both abstract and non-abstract methods.
- **Variables**: An interface can only have static final variables, while an abstract class can have instance variables.
- **Access Modifiers**: An interface can only have public methods, while an abstract class can have various access modifiers.
