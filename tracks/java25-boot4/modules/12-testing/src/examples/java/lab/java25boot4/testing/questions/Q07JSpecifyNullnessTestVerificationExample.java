package lab.java25boot4.testing.questions;

import org.jspecify.annotations.Nullable;

/** Q07: How do JSpecify nullness annotations impact unit and integration test assertions? */
public class Q07JSpecifyNullnessTestVerificationExample {

    public record UserProfile(String username, @Nullable String bio) {}

    public static void main(String[] args) {
        UserProfile userWithBio = new UserProfile("alice", "Senior Engineer");
        UserProfile userWithoutBio = new UserProfile("bob", null);

        boolean hasBioNonNull = userWithBio.bio() != null;
        boolean hasBioNull = userWithoutBio.bio() == null;

        System.out.println(
                "User with bio non-null: " + hasBioNonNull); // User with bio non-null: true
        System.out.println("User without bio null: " + hasBioNull); // User without bio null: true
    }
}
