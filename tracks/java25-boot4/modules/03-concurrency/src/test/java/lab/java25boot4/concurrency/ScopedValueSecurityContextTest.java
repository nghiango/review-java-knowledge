package lab.java25boot4.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;
import lab.java25boot4.concurrency.ScopedValueSecurityContext.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScopedValueSecurityContextTest {

    @Test
    @DisplayName("Should retrieve bound principal within execution scope")
    void runAs_withPrincipal_principalIsAccessible() throws Exception {
        var principal = new UserPrincipal("usr_42", "ADMIN");

        String result =
                ScopedValueSecurityContext.runAs(
                        principal,
                        () -> {
                            var current = ScopedValueSecurityContext.getRequiredPrincipal();
                            return current.userId() + ":" + current.role();
                        });

        assertThat(result).isEqualTo("usr_42:ADMIN");
    }

    @Test
    @DisplayName("Should throw exception when accessing principal outside bound scope")
    void getRequiredPrincipal_outsideScope_throwsIllegalStateException() {
        assertThatThrownBy(ScopedValueSecurityContext::getRequiredPrincipal)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No principal bound");
    }

    @Test
    @DisplayName("Should support re-binding with inner scope shadowing")
    void runAs_nestedScopes_innerShadowsOuter() {
        var outerUser = new UserPrincipal("usr_outer", "USER");
        var innerUser = new UserPrincipal("usr_inner", "SUPERADMIN");
        AtomicReference<String> observedInner = new AtomicReference<>();
        AtomicReference<String> observedOuterAfter = new AtomicReference<>();

        ScopedValueSecurityContext.runAs(
                outerUser,
                () -> {
                    ScopedValueSecurityContext.runAs(
                            innerUser,
                            () -> {
                                observedInner.set(
                                        ScopedValueSecurityContext.getRequiredPrincipal().userId());
                            });
                    observedOuterAfter.set(
                            ScopedValueSecurityContext.getRequiredPrincipal().userId());
                });

        assertThat(observedInner.get()).isEqualTo("usr_inner");
        assertThat(observedOuterAfter.get()).isEqualTo("usr_outer");
    }
}
