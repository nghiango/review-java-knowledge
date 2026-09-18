package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q09ClassLoadingLifecycleExample {
    private Q09ClassLoadingLifecycleExample() {}

    public static class Sample {
        public static int count = 10;

        static {
            count = 20; // Runs during Initialization (<clinit>) phase after Loading and Linking
        }
    }

    public static void main(String[] args) {
        int initialValue = Sample.count; // 20 (loaded, linked, and initialized)
    }
}
