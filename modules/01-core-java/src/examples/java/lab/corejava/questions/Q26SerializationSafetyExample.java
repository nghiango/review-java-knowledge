package lab.corejava.questions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressWarnings({"unused", "serial"})
public final class Q26SerializationSafetyExample {
    private Q26SerializationSafetyExample() {}

    // Insecure legacy class: deserialization bypasses constructor validation!
    public static class LegacyAccount implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int balance;

        public LegacyAccount(int balance) {
            if (balance < 0) {
                throw new IllegalArgumentException("Balance cannot be negative");
            }
            this.balance = balance;
        }

        public int getBalance() {
            return balance;
        }
    }

    // Safe modern Record: deserialization MUST invoke canonical constructor, enforcing invariants
    public record SafeAccount(int balance) implements Serializable {
        private static final long serialVersionUID = 1L;

        public SafeAccount {
            if (balance < 0) {
                throw new IllegalArgumentException("Balance cannot be negative");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        SafeAccount account = new SafeAccount(500);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(account);
        }

        try (ObjectInputStream ois =
                new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            SafeAccount restored = (SafeAccount) ois.readObject();
            int bal = restored.balance(); // 500 (canonical constructor validated fields upon
            // deserialization)
        }
    }
}
