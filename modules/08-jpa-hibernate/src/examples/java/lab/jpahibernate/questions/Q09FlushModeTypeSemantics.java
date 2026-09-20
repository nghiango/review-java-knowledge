package lab.jpahibernate.questions;

import jakarta.persistence.FlushModeType;

public class Q09FlushModeTypeSemantics {

    public static void main(String[] args) {
        // FlushModeType.AUTO (Default): Flushes before query execution (if query overlaps dirty
        // entities) and before transaction commit
        FlushModeType defaultFlushMode = FlushModeType.AUTO;
        // FlushModeType.COMMIT: Flushes only upon transaction commit (queries might read stale DB
        // data if not manually flushed)
        FlushModeType commitFlushMode = FlushModeType.COMMIT;

        boolean autoFlushesBeforeOverlappingQueries =
                (defaultFlushMode == FlushModeType.AUTO); // true
        boolean commitDelaysFlushUntilCommit = (commitFlushMode == FlushModeType.COMMIT); // true

        System.out.println("Default flush mode: " + defaultFlushMode); // Default flush mode: AUTO
        System.out.println(
                "AUTO flushes before overlapping query: "
                        + autoFlushesBeforeOverlappingQueries); // AUTO flushes before overlapping
        // query: true
        System.out.println(
                "COMMIT delays flush until commit: "
                        + commitDelaysFlushUntilCommit); // COMMIT delays flush until commit: true
    }
}
