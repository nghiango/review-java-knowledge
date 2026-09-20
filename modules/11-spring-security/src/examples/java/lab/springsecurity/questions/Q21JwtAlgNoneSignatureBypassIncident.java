package lab.springsecurity.questions;

public class Q21JwtAlgNoneSignatureBypassIncident {

    public static void main(String[] args) {
        // Incident: Attacker modifies JWT header to {"alg":"none"} and strips signature
        String forgedHeader = "{\"alg\":\"none\",\"typ\":\"JWT\"}";

        // Vulnerable parser accepts alg:none
        boolean vulnerableValidatorAccepted =
                forgedHeader.contains("\"alg\":\"none\""); // true (critical vulnerability)

        // Hardened parser explicitly rejects "none"
        boolean isAlgSupported =
                !forgedHeader.contains("\"alg\":\"none\"")
                        && forgedHeader.contains("\"alg\":\"HS256\""); // false
        boolean signatureRejected = !isAlgSupported; // true

        System.out.println(
                "Vulnerable parser accepted alg:none: "
                        + vulnerableValidatorAccepted); // Vulnerable parser accepted alg:none: true
        System.out.println(
                "Hardened parser rejected unsigned token: "
                        + signatureRejected); // Hardened parser rejected unsigned token: true
    }
}
