package lab.distributeddata.questions;

public class Q09EventualConsistencyReadYourOwnWritesExample {

    record ReadYourOwnWritesContext(long clientMutationVersion, long replicaObservedVersion) {
        boolean isSafeToReadFromReplica() {
            return replicaObservedVersion >= clientMutationVersion;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Client updated profile to version 5.
        // Replica A is caught up to version 5; Replica B is lagging at version 4.
        ReadYourOwnWritesContext replicaA = new ReadYourOwnWritesContext(5, 5);
        ReadYourOwnWritesContext replicaB = new ReadYourOwnWritesContext(5, 4);

        boolean safeOnA = replicaA.isSafeToReadFromReplica(); // true
        boolean routeToPrimaryOnB = !replicaB.isSafeToReadFromReplica(); // true

        System.out.println(
                "Replica A provides read-your-own-writes: "
                        + safeOnA
                        + ", Replica B requires master route: "
                        + routeToPrimaryOnB);
    }
}
