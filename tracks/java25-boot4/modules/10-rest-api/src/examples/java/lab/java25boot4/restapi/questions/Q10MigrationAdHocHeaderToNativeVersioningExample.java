package lab.java25boot4.restapi.questions;

import java.util.Set;

/**
 * Q10: How do you refactor an ad-hoc @RequestHeader API version check into declarative Spring
 * Framework 7 route mappings?
 */
public class Q10MigrationAdHocHeaderToNativeVersioningExample {

    public static final Set<String> SUPPORTED_VERSIONS = Set.of("1.0", "2.0");

    public static boolean validateRequestedVersion(String version) {
        return SUPPORTED_VERSIONS.contains(version);
    }

    public static void main(String[] args) {
        String validVersion = "2.0";
        String obsoleteVersion = "0.9";

        boolean validResult = validateRequestedVersion(validVersion);
        boolean obsoleteResult = validateRequestedVersion(obsoleteVersion);

        System.out.println("Version 2.0 valid: " + validResult); // true
        System.out.println("Version 0.9 valid: " + obsoleteResult); // false
        System.out.println("Supported count: " + SUPPORTED_VERSIONS.size()); // 2
    }
}
