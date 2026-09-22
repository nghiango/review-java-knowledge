package lab.java25boot4.whatsnew.questions;

/** Q6: what do flexible constructor bodies allow? */
public class Q06FlexibleConstructorBodiesExample {

    static class Base {
        private final String normalized;

        Base(String normalized) {
            this.normalized = normalized;
        }

        String normalized() {
            return normalized;
        }
    }

    static class EmailAddress extends Base {
        EmailAddress(String value) {
            String trimmed = value == null ? "" : value.trim(); // statement BEFORE super(...)
            if (!trimmed.contains("@")) {
                throw new IllegalArgumentException("invalid email: " + trimmed);
            }
            super(trimmed.toLowerCase());
        }
    }

    public static void main(String[] args) {
        System.out.println(new EmailAddress("  A@B.com ").normalized()); // a@b.com

        try {
            new EmailAddress("nope");
        } catch (IllegalArgumentException invalid) {
            System.out.println(invalid.getMessage()); // invalid email: nope
        }
    }
}
