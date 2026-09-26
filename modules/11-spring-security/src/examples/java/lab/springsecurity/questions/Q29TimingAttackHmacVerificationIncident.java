package lab.springsecurity.questions;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Q29: Production Incident: Timing attack on HMAC token verification leaks secrets. How do you
 * diagnose and remediate?
 */
public class Q29TimingAttackHmacVerificationIncident {

    // Vulnerable: Short-circuiting string equals leaks timing information per matching character
    static boolean vulnerableEquals(String a, String b) {
        return a.equals(b); // Leaks ~10-50ns per matched prefix character
    }

    // Hardened: Constant-time byte comparison using MessageDigest.isEqual
    static boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes); // Constant time comparison
    }

    public static void main(String[] args) {
        String expectedSignature = "abcdef9876543210fedcba";
        String attackerGuess = "abcdef0000000000000000";

        boolean vulnCheck = vulnerableEquals(expectedSignature, attackerGuess); // false
        boolean secureCheck = constantTimeEquals(expectedSignature, attackerGuess); // false

        System.out.println("Vulnerable Check: " + vulnCheck); // false (but leaked 6 chars timing)
        System.out.println("Constant-Time Check: " + secureCheck); // false (zero timing side-channel)
    }
}
