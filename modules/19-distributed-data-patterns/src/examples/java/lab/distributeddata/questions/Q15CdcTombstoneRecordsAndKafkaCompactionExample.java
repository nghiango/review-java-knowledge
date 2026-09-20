package lab.distributeddata.questions;

public class Q15CdcTombstoneRecordsAndKafkaCompactionExample {

    record KafkaRecord(String key, String value, boolean isTombstone) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In log compacted Kafka topics, deleting a record in the source DB produces a "tombstone"
        // (a message with the original key and a null payload), signaling topic compaction to
        // delete the key.
        KafkaRecord upsert = new KafkaRecord("user-1", "{\"name\":\"Alice\"}", false);
        KafkaRecord tombstone = new KafkaRecord("user-1", null, true);

        boolean isCompactionTombstone =
                tombstone.isTombstone() && tombstone.value() == null; // true
        System.out.println(
                "Tombstone record signals key deletion to Kafka log cleaner: "
                        + isCompactionTombstone);
    }
}
