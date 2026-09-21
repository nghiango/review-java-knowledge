package lab.java25boot4.concurrency.questions;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

public class Q01StructuredConcurrencyBasicsExample {

    public static void main(String[] args) throws Exception {
        try (var scope = StructuredTaskScope.open()) {
            Subtask<String> subtask1 = scope.fork(() -> "User");
            Subtask<String> subtask2 = scope.fork(() -> "Orders");

            scope.join();

            System.out.println(subtask1.get()); // User
            System.out.println(subtask2.get()); // Orders
        }
    }
}
