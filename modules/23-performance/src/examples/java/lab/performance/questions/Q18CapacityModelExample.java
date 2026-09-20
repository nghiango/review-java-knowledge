package lab.performance.questions;

public final class Q18CapacityModelExample {
    public static void main(String[] args) {
        int replicas = 6, poolPerReplica = 15;
        int totalConnections = replicas * poolPerReplica; // 90
        System.out.println(totalConnections);
    }
}
