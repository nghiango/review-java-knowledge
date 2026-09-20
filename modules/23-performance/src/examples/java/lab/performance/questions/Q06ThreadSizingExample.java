package lab.performance.questions;

import lab.performance.threadpool.WorkloadProfile;

public final class Q06ThreadSizingExample {
    public static void main(String[] args) {
        int workers = new WorkloadProfile(8, 0.5, 100).workerCount(); // 12
        System.out.println(workers);
    }
}
