package lab.java25boot4.whatsnew.securitygate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceAuthorizationGuardTest {

    private final ResourceAuthorizationGuard guard = new ResourceAuthorizationGuard();
    private final Caller admin = new Caller("alice", Set.of("admin.operations"));

    @Test
    void callerWithAuthority_isAuthorized() {
        assertThat(guard.isAuthorized(admin, "admin.operations")).isTrue();
    }

    @Test
    void callerWithoutAuthority_isDenied() {
        assertThat(guard.isAuthorized(new Caller("bob", Set.of("read")), "admin.operations"))
                .isFalse();
    }

    @Test
    void missingCaller_isDenied_notAllowed() {
        assertThat(guard.isAuthorized(null, "admin.operations")).isFalse();
    }

    @Test
    void checkAuthorized_denied_throwsWithCallerContext() {
        assertThatThrownBy(() -> guard.checkAuthorized(null, "admin.operations"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("<anonymous>");
    }

    @Test
    void caller_blankId_isRejected() {
        assertThatThrownBy(() -> new Caller(" ", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void caller_nullAuthorities_becomesEmptySet() {
        assertThat(new Caller("carol", null).authorities()).isEmpty();
    }
}
