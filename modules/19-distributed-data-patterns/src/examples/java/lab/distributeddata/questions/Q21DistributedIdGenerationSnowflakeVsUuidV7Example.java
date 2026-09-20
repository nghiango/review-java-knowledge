package lab.distributeddata.questions;

public class Q21DistributedIdGenerationSnowflakeVsUuidV7Example {

    record IdScheme(String name, boolean timeOrdered, boolean bTreeIndexFriendly) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // UUIDv4 causes B-Tree index fragmentation and random I/O splits.
        // UUIDv7 and Twitter Snowflake are monotonically time-ordered, maximizing B-Tree clustering
        // performance.
        IdScheme uuidV4 = new IdScheme("UUIDv4", false, false);
        IdScheme uuidV7 = new IdScheme("UUIDv7", true, true);
        IdScheme snowflake = new IdScheme("Snowflake", true, true);

        boolean v7IsOrdered = uuidV7.timeOrdered() && uuidV7.bTreeIndexFriendly(); // true
        boolean snowflakeIsOrdered =
                snowflake.timeOrdered() && snowflake.bTreeIndexFriendly(); // true

        System.out.println(
                "Time-ordered ID generation preserves B-Tree locality: "
                        + (v7IsOrdered && snowflakeIsOrdered));
    }
}
