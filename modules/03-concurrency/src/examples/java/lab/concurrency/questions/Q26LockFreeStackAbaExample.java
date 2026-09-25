package lab.concurrency.questions;

import java.util.concurrent.atomic.AtomicStampedReference;

@SuppressWarnings("unused")
public final class Q26LockFreeStackAbaExample {
    private Q26LockFreeStackAbaExample() {}

    private static class Node<E> {
        final E item;
        Node<E> next;

        Node(E item) {
            this.item = item;
        }
    }

    // Lock-free stack using AtomicStampedReference to defeat the ABA problem:
    // Pair of (Node reference, integer stamp version)
    public static class LockFreeStack<E> {
        private final AtomicStampedReference<Node<E>> top = new AtomicStampedReference<>(null, 0);

        public void push(E item) {
            Node<E> newHead = new Node<>(item);
            int[] stampHolder = new int[1];
            Node<E> currentHead;
            do {
                currentHead = top.get(stampHolder);
                newHead.next = currentHead;
            } while (!top.compareAndSet(currentHead, newHead, stampHolder[0], stampHolder[0] + 1));
        }

        public E pop() {
            int[] stampHolder = new int[1];
            Node<E> currentHead;
            Node<E> nextHead;
            do {
                currentHead = top.get(stampHolder);
                if (currentHead == null) {
                    return null;
                }
                nextHead = currentHead.next;
            } while (!top.compareAndSet(currentHead, nextHead, stampHolder[0], stampHolder[0] + 1));

            return currentHead.item;
        }
    }

    public static void main(String[] args) {
        LockFreeStack<String> stack = new LockFreeStack<>();
        stack.push("A");
        stack.push("B");

        String popped1 = stack.pop(); // "B"
        String popped2 = stack.pop(); // "A"
        String empty = stack.pop(); // null
    }
}
