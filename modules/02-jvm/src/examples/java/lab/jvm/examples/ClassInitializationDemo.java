package lab.jvm.examples;

public final class ClassInitializationDemo {
    private ClassInitializationDemo() {}

    public static void main(String[] args) {
        System.out.println("main starts");
        System.out.println(Child.message());
    }

    private static class Parent {
        static {
            System.out.println("Parent static initializer");
        }

        static String parentMessage() {
            return "parent ready";
        }
    }

    private static final class Child extends Parent {
        static {
            System.out.println("Child static initializer");
        }

        static String message() {
            return parentMessage() + ", child ready";
        }
    }
}
