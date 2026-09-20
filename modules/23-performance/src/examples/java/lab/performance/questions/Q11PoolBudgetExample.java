package lab.performance.questions;

import lab.performance.connectionpool.ConnectionPoolBudget;

public final class Q11PoolBudgetExample {
    public static void main(String[] args) {
        int perInstance = new ConnectionPoolBudget(100, 10, 6).maximumPoolSize(); // 15
        System.out.println(perInstance);
    }
}
