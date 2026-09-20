package lab.springtransactions.questions;

public class Q04IsolationLevelsAnomaliesExample {

    record IsolationLevel(
            String name,
            boolean allowsDirtyRead,
            boolean allowsNonRepeatableRead,
            boolean allowsPhantomRead) {}

    public static void main(String[] args) {
        // Standard SQL ANSI Isolation levels
        IsolationLevel readUncommitted = new IsolationLevel("READ_UNCOMMITTED", true, true, true);
        IsolationLevel readCommitted = new IsolationLevel("READ_COMMITTED", false, true, true);
        IsolationLevel repeatableRead = new IsolationLevel("REPEATABLE_READ", false, false, true);
        IsolationLevel serializable = new IsolationLevel("SERIALIZABLE", false, false, false);

        boolean ruAllowsDirty = readUncommitted.allowsDirtyRead(); // true
        boolean rcPreventsDirty = !readCommitted.allowsDirtyRead(); // true
        boolean rrPreventsNonRepeatable = !repeatableRead.allowsNonRepeatableRead(); // true
        boolean serPreventsAll =
                !serializable.allowsDirtyRead()
                        && !serializable.allowsNonRepeatableRead()
                        && !serializable.allowsPhantomRead(); // true

        System.out.println(
                "RU allows dirty: "
                        + ruAllowsDirty
                        + ", RC prevents dirty: "
                        + rcPreventsDirty
                        + ", RR prevents non-repeatable: "
                        + rrPreventsNonRepeatable
                        + ", Serializable strict: "
                        + serPreventsAll);
    }
}
