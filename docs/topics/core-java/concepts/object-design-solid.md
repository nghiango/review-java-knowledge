## Object design and SOLID

### Object-Oriented Principles

**Encapsulation** means combining data and related behavior inside a class while controlling access to the internal state. For example, a `BankAccount` can keep its balance private and provide `deposit()` and `withdraw()` methods that enforce business rules.

**Abstraction** means exposing essential behavior while hiding implementation details. For example, a `PaymentService` interface can define a `pay()` method without exposing how credit-card or bank-transfer payments are processed.

**Polymorphism** allows different implementations to be used through a common type. For example, both `CreditCardPayment` and `BankTransferPayment` can implement the `PaymentService` interface, and the application can invoke `pay()` without knowing the concrete implementation.

**Inheritance** allows a subclass to reuse and extend the behavior of a parent class. For example, `SavingsAccount` can extend `BankAccount`. However, inheritance should represent a genuine "is-a" relationship, not just code reuse.

#### Model interview answer

??? question "Sample answer"
    Object-Oriented Programming has four main principles: encapsulation, abstraction, inheritance, and polymorphism.

    First, encapsulation protects an object's internal state by restricting direct access and exposing controlled methods. For example, a BankAccount class should provide deposit and withdraw methods rather than exposing its balance directly.

    Second, abstraction hides implementation details and exposes essential behavior through interfaces or abstract classes. For example, a PaymentProcessor interface allows different payment implementations without changing the client code.

    Third, inheritance allows a subclass to reuse and extend behavior from a superclass. However, I prefer composition when there is no clear IS-A relationship because it reduces coupling.

    Finally, polymorphism allows the same method call to behave differently based on the implementation. Java supports method overloading and method overriding.

    Together, these principles help us build reusable, maintainable, and extensible applications.

### SOLID Principles

**SOLID** are five fundamental principles of object-oriented design and programming.

#### SRP - Single Responsibility Principle

- Definition: A class should have only one reason to change, meaning it should focus on a single job or responsibility.
- Impact: Reduces coupling and makes code more maintainable.

??? example "Example"

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

??? example "Example"

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

??? example "Example"

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

??? example "Example"

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

??? example "Example"

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
