package lab.corejava.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q03RecordImmutabilityExample {
    private Q03RecordImmutabilityExample() {}

    public record UserProfile(String id, List<String> roles) {
        public UserProfile {
            roles = List.copyOf(roles);
        }
    }

    public static void main(String[] args) {
        List<String> mutableRoles = new ArrayList<>(List.of("USER"));
        UserProfile profile = new UserProfile("usr-1", mutableRoles);

        mutableRoles.add("ADMIN"); // mutates external list

        int count =
                profile.roles().size(); // 1 (record snapshot is isolated from external mutation)
        String role = profile.roles().get(0); // "USER"
    }
}
