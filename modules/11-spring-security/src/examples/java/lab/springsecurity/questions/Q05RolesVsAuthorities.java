package lab.springsecurity.questions;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class Q05RolesVsAuthorities {

    public static void main(String[] args) {
        // In Spring Security, a "Role" is simply a GrantedAuthority prefixed with "ROLE_"
        List<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("order:read"),
                        new SimpleGrantedAuthority("order:write"));

        // hasRole("ADMIN") checks for "ROLE_ADMIN" authority
        boolean hasAdminRole =
                authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); // true

        // hasAuthority("order:write") checks for exact authority name
        boolean hasWriteAuthority =
                authorities.stream().anyMatch(a -> a.getAuthority().equals("order:write")); // true

        boolean hasBareRoleAuthority =
                authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN")); // false

        System.out.println("Has Role ADMIN: " + hasAdminRole); // Has Role ADMIN: true
        System.out.println(
                "Has Authority order:write: "
                        + hasWriteAuthority); // Has Authority order:write: true
        System.out.println(
                "Contains bare string ADMIN authority: "
                        + hasBareRoleAuthority); // Contains bare string ADMIN authority: false
    }
}
