package lab.jvm.questions;

@SuppressWarnings("unused")
public final class Q10ClassInitializationTriggersExample {
    private Q10ClassInitializationTriggersExample() {}

    public static class Config {
        public static final int INLINED_CONST =
                42; // compile-time constant (does NOT trigger <clinit>)
        public static int ACTIVE_MUTABLE = 100; // access triggers <clinit>

        static {
            ACTIVE_MUTABLE = 200;
        }
    }

    public static void main(String[] args) {
        int constVal =
                Config.INLINED_CONST; // 42 (inlined bytecode literal, Config not initialized yet)
        int activeVal = Config.ACTIVE_MUTABLE; // 200 (triggers class initialization)
    }
}
