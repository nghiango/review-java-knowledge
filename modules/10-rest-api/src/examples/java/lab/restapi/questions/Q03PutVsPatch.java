package lab.restapi.questions;

import java.util.HashMap;
import java.util.Map;

public class Q03PutVsPatch {

    public static void main(String[] args) {
        // Initial Resource State
        Map<String, String> userResource = new HashMap<>();
        userResource.put("name", "Alice");
        userResource.put("email", "alice@example.com");
        userResource.put("role", "EDITOR");

        // PUT: Complete replacement of target resource. Any missing fields in payload are set to
        // null/default.
        Map<String, String> putPayload =
                Map.of("name", "Alice Smith", "email", "alice.smith@example.com");
        Map<String, String> afterPut = applyPut(userResource, putPayload);
        boolean putReplacedRole =
                !afterPut.containsKey("role"); // true (role was omitted in payload, so removed)

        // PATCH: Partial modification. Modifies only explicitly specified fields in the delta
        // payload.
        Map<String, String> patchPayload = Map.of("name", "Alice Johnson");
        Map<String, String> afterPatch = applyPatch(userResource, patchPayload);
        boolean patchPreservedEmail = "alice@example.com".equals(afterPatch.get("email")); // true

        System.out.println(
                "PUT replaced all fields: " + putReplacedRole); // PUT replaced all fields: true
        System.out.println(
                "PATCH preserved omitted fields: "
                        + patchPreservedEmail); // PATCH preserved omitted fields: true
    }

    public static Map<String, String> applyPut(
            Map<String, String> current, Map<String, String> fullPayload) {
        return new HashMap<>(fullPayload);
    }

    public static Map<String, String> applyPatch(
            Map<String, String> current, Map<String, String> delta) {
        Map<String, String> copy = new HashMap<>(current);
        copy.putAll(delta);
        return copy;
    }
}
