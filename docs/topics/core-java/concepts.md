# Core Java Concepts

## Object design and SOLID

### Object-Oriented Principles

Encapsulation protects invariants, not merely fields.

Abstraction exposes a stable contract.

Polymorphism lets callers depend on that contract. Prefer composition when a relationship is "uses/has" or when subtype equality would be ambiguous.

Inheritance fits genuine substitutability.

### SOLID Principles

SOLID are five fundamental principles of object-oriented design and programming.

#### SRP - Single Responsibility Principle

- Definition: A class should have only one reason to change, meaning it should focus on a single job or responsibility.
- Impact: Reduces coupling and makes code more maintainable.

??? "Example"
    ```java
    // Bad example, UserHandler has multiple responsibilities
    // It handles user data and also database operations and email sending
    public class UserHandler {
        private String name;
        private String email;
        
        public void saveToDatabase() {
            // Database logic
        }
        
        public void sendEmail() {
            // Email logic
        }
    }

    // Good example, User only handles user data
    // Database operations and email sending are handled by separate classes
    public class User {
        private String name;
        private String email;
    }

    public class UserRepository {
        public void save(User user) {
            // Database logic
        }
    }

    public class EmailService {
        public void send(User user) {
            // Email logic
        }
    }
    ```

#### OCP - Open/Closed Principle

- Definition: Protects stable policy with extension points. It means that classes should be open for extension but closed for modification.
- Impact: Makes code more maintainable and extensible.

??? "Example"
    ```java
    // Bad example, PaymentProcessor has multiple responsibilities
    // It handles different payment methods and also logging
    public class PaymentProcessor {
        public void processPayment(PaymentMethod method, double amount) {
            // Payment logic
            System.out.println("Processing payment of " + amount);
        }
        
        public void logPayment(PaymentMethod method, double amount) {
            // Logging logic
            System.out.println("Logged payment of " + amount);
        }
    }

    // Good example, PaymentProcessor only handles payment processing
    // Logging is handled by a separate class
    public class PaymentProcessor {
        public void processPayment(PaymentMethod method, double amount) {
            // Payment logic
            System.out.println("Processing payment of " + amount);
        }
    }

    public class PaymentLogger {
        public void logPayment(PaymentMethod method, double amount) {
            // Logging logic
            System.out.println("Logged payment of " + amount);
        }
    }
    ```

#### LSP - Liskov Substitution Principle

- Definition: Demands substitutability. It means that objects of a superclass should be replaceable with objects of its subclasses without breaking the application.
- Impact: Ensures that subclasses can be used interchangeably with their base classes.

??? "Example"
    ```java
    // Bad example, Rectangle and Square don't follow LSP
    // Square breaks the contract of Rectangle
    public class Rectangle {
        protected int width;
        protected int height;
        
        public void setWidth(int width) {
            this.width = width;
        }
        
        public void setHeight(int height) {
            this.height = height;
        }
        
        public int getArea() {
            return width * height;
        }
    }
    
    public class Square extends Rectangle {
        public Square(int side) {
            super(side, side);
        }
        
        @Override
        public void setWidth(int width) {
            this.width = width;
            this.height = width; // This breaks the contract of Rectangle
        }
        
        @Override
        public void setHeight(int height) {
            this.width = height;
            this.height = height; // This breaks the contract of Rectangle
        }
    }

    // Good example, Rectangle and Square follow LSP
    // Both have the same contract and can be used interchangeably
    public abstract class Shape {
        public abstract int getArea();
    }
    
    public class Rectangle extends Shape {
        private int width;
        private int height;
        
        public Rectangle(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        @Override
        public int getArea() {
            return width * height;
        }
    }
    
    public class Square extends Shape {
        private int side;
        
        public Square(int side) {
            this.side = side;
        }
        
        @Override
        public int getArea() {
            return side * side;
        }
    }
    ```

#### ISP - Interface Segregation Principle

- Definition: Avoids forcing unused capabilities. It means that clients should not be forced to depend on interfaces they don't use.
- Impact: Makes code more maintainable and extensible.

??? "Example"
    ```java
    // Bad example
    public interface Worker {
        void work();
        void feed();
    }
    
    public class Robot implements Worker {
        @Override
        public void work() {
            System.out.println("Robot is working");
        }
        
        @Override
        public void feed() {
            // Robot doesn't need to feed, but it has to implement this method
            throw new UnsupportedOperationException("Robot doesn't need to feed");
        }
    }

    // Good example
    public interface Workable {
        void work();
    }
    
    public interface Feedable {
        void feed();
    }

    public class Robot implements Workable {
        @Override
        public void work() {
            System.out.println("Robot is working");
        }
    }
    
    public class Human implements Workable, Feedable {
        @Override
        public void work() {
            System.out.println("Human is working");
        }
        
        @Override
        public void feed() {
            System.out.println("Human is feeding");
        }
    }
    ```

#### DIP - Dependency Inversion Principle

- Definition: Points policy toward abstractions. It means that high-level modules should not depend on low-level modules. Both should depend on abstractions.
- Impact: Makes code more maintainable and extensible.

??? "Example"
    ```java
    // Bad example
    public class Car {
        private Engine engine = new V8Engine(); // Tight coupling
        
        public Car() {
        }
    }
    
    // Good example
    public interface Engine {
        void start();
    }
    public class Car {
        private final Engine engine; // Depends on abstraction
        
        public Car(Engine engine) { // Dependency injection
            this.engine = engine;
        }
    }
    ```

Too many tiny interfaces can be as costly as a god class.

## Equality, hashing and ordering

- `==` compares primitive values or reference identity.
- `equals` defines logical equality. It must be
reflexive, symmetric, transitive, consistent and false for null. 

??? "Example"
    ```java
    String s1 = "hello";
    String s2 = "hello";
    System.out.println(s1 == s2); // true (string pool)
    System.out.println(s1.equals(s2)); // true

    String s3 = new String("hello");
    String s4 = new String("hello");
    System.out.println(s3 == s4); // false (different objects)
    System.out.println(s3.equals(s4)); // true (same content)

    String s5 = "hello";
    String s6 = new String("hello");
    System.out.println(s5 == s6); // false (different objects)
    System.out.println(s5.equals(s6)); // true (same content)
    ```

- Equal objects must have equal hash codes; unequal objects may collide. Equality/hash state used as a HashMap key must not change while
stored.
- `Comparable` defines a type's natural order; `Comparator` defines external orders. A sorted set uses
comparison equality (`compare == 0`) for uniqueness, so inconsistency with `equals` can surprise
callers even though Java permits it.

??? "Example"
    ```java
    public class Person implements Comparable<Person> {
        private final String name;
        private final int age;
        
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Person person = (Person) obj;
            return age == person.age && Objects.equals(name, person.name);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
        
        @Override
        public int compareTo(Person other) {
            int nameCompare = name.compareTo(other.name);
            if (nameCompare != 0) return nameCompare;
            return Integer.compare(age, other.age);
        }
    }
    ```

## Immutability and Java 21 data types

An immutable object has stable observable state: final references are insufficient if they point to
mutable collections. Copy mutable inputs and outputs. Records provide final components and value
semantics but are only shallowly immutable. Enums model a closed set of instances; sealed classes
model a closed hierarchy with variant-specific state. Java 21 pattern matching makes exhaustive
`switch` over sealed hierarchies practical.

## Strings, values and modifiers

String is immutable and may share interned literals. Use StringBuilder for iterative construction;
modern `+` expressions may be optimized, but concatenation in a loop repeatedly creates values.
Java is always pass-by-value: an object reference is itself a copied value, so a method can mutate
the referenced object but cannot replace the caller's variable. `final` prevents reassignment or
override depending on context; `static` belongs to the class, not an instance.

`var` changes local syntax, not static typing. Use it when the initializer makes the type obvious.
Use `java.time` immutable types and explicit zones; avoid legacy mutable date APIs.

## Exceptions and resources

Checked exceptions make a recoverable condition part of the signature; unchecked exceptions often
represent contract violations or failures callers cannot locally recover from. The boundary matters
more than taxonomy: catch only when the layer can add context, recover or translate. Preserve the
cause.

Try-with-resources closes in reverse declaration order. If body and close both fail, the body failure
is primary and close failures are suppressed. Resource ownership must be explicit.

## Optional

Optional is primarily a return type for expected absence. It is not a universal null replacement:
fields and parameters often create double absence, serialization friction and noisy calls. Prefer
`map`, `flatMap`, `filter`, `orElseGet` and contextual `orElseThrow`; avoid unproved `get()`.

## Collections

Choose by contract: List preserves position/duplicates; Set enforces uniqueness; Map associates
keys; Queue/Deque model processing order. ArrayList gives O(1) indexed access and amortized append
but O(n) middle insertion. LinkedList rarely wins due to traversal and locality costs. Hash
collections offer expected O(1) lookup under stable hashing; tree collections offer O(log n) sorted
operations. Sequenced collections expose first/last/reversed order consistently.

Fail-fast iterators detect structural modification on a best-effort basis; they are bug detectors,
not concurrency controls. ConcurrentHashMap coordinates concurrent access but still requires stable
keys and atomic compound operations (`compute`, `merge`) where appropriate.

## Generics and PECS

Generics provide compile-time type safety but most type parameters are erased. Invariance means
`List<Integer>` is not a subtype of `List<Number>`. A producer is `? extends T`; a consumer is
`? super T`. Wildcards express use-site flexibility; a type parameter is preferable when multiple
positions must share the same unknown type.

## Streams

A stream describes a pipeline; it does not store data. Intermediate operations are lazy and a
terminal operation drives traversal. Stateless operations handle each element independently;
stateful operations such as `sorted` and `distinct` may buffer or create barriers. Short-circuiting
can avoid full traversal.

Keep pipelines non-interfering and stateless. Collect through framework collectors rather than
mutating external containers. Parallel streams are useful only for sufficiently large, splittable,
CPU-bound, side-effect-free work after measurement; they are a poor default for blocking I/O or
request-path work sharing the common pool.

## Related

- [Internals](internals.md)
- [Code review](code-review.md)
- [Production](production.md)
