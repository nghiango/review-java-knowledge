package lab.performance.questions;

public final class Q22HikariIncidentExample {
    public static void main(String[] args) {
        int active = 20, max = 20, pending = 75, databaseCpu = 20;
        boolean heldOutsideDatabase = active == max && pending > 0 && databaseCpu < 50; // true
        System.out.println(heldOutsideDatabase);
    }
}
