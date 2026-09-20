package lab.springsecurity.questions;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class Q03PasswordEncoderMechanics {

    public static void main(String[] args) {
        PasswordEncoder delegatingEncoder =
                PasswordEncoderFactories.createDelegatingPasswordEncoder();

        String rawSecret = "AdminPassword2026!";
        String encodedHash = delegatingEncoder.encode(rawSecret); // e.g. "{bcrypt}$2a$10$..."

        boolean hasPrefix = encodedHash.startsWith("{bcrypt}"); // true
        boolean matchesCorrect = delegatingEncoder.matches(rawSecret, encodedHash); // true
        boolean matchesWrong = delegatingEncoder.matches("WrongPassword", encodedHash); // false

        System.out.println(
                "Encoded hash starts with algorithm ID: "
                        + hasPrefix); // Encoded hash starts with algorithm ID: true
        System.out.println(
                "Matches valid password: " + matchesCorrect); // Matches valid password: true
        System.out.println(
                "Rejects invalid password: " + matchesWrong); // Rejects invalid password: false
    }
}
