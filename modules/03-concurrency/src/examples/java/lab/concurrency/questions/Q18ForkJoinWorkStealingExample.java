package lab.concurrency.questions;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/** Q18: Demonstrates ForkJoinPool recursive task decomposition and work stealing. */
@SuppressWarnings("unused")
public class Q18ForkJoinWorkStealingExample {

    static class SumTask extends RecursiveTask<Long> {
        private final long[] numbers;
        private final int start;
        private final int end;

        SumTask(long[] numbers, int start, int end) {
            this.numbers = numbers;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            if (end - start <= 2) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += numbers[i];
                }
                return sum;
            }
            int mid = start + (end - start) / 2;
            SumTask left = new SumTask(numbers, start, mid);
            SumTask right = new SumTask(numbers, mid, end);
            left.fork(); // Pushes to local worker deque for work-stealing by idle threads
            long rightResult = right.compute();
            long leftResult = left.join();
            return leftResult + rightResult;
        }
    }

    public static void main(String[] args) {
        long[] array = {1, 2, 3, 4, 5, 6, 7, 8};
        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            Long total = pool.invoke(new SumTask(array, 0, array.length)); // 36L
        }
    }
}
