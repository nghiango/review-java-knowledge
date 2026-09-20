package lab.testing.questions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lab.testing.fulfilment.InventoryClient;
import lab.testing.fulfilment.InventoryResponse;
import lab.testing.fulfilment.OrderFulfilmentService;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.exceptions.misusing.UnnecessaryStubbingException;
import org.mockito.quality.Strictness;

/**
 * Q06: Mockito strict stubs — what the strictness level actually checks.
 *
 * <p>{@code @ExtendWith(MockitoExtension.class)} opens a {@link MockitoSession} per test with
 * {@link Strictness#STRICT_STUBS} and calls {@code finishMocking()} afterwards; a stub that was
 * never used then fails the test with an {@link UnnecessaryStubbingException}. This example drives
 * that session directly, so the mechanism is visible instead of hidden behind the extension (and
 * the classpath stays on {@code mockito-core}).
 */
public class Q06MockitoStrictStubs {

    // The unused stub is the subject of this example, so "this mock is never passed to production
    // code" is exactly the situation being demonstrated rather than a defect to fix.
    @SuppressWarnings("MockNotUsedInProduction")
    public static void main(String[] args) {
        // 1. A stub that the test uses: the session finishes clean.
        MockitoSession usedSession =
                Mockito.mockitoSession().strictness(Strictness.STRICT_STUBS).startMocking();
        InventoryClient usedClient = mock(InventoryClient.class);
        when(usedClient.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 3));
        boolean canFulfil = new OrderFulfilmentService(usedClient).canFulfil("A-1", 3); // true
        String usedOutcome = finish(usedSession); // "no failure"

        // 2. The same stub left unused: strict stubs turn the silent stub into a failure.
        MockitoSession unusedSession =
                Mockito.mockitoSession().strictness(Strictness.STRICT_STUBS).startMocking();
        InventoryClient unusedClient = mock(InventoryClient.class);
        when(unusedClient.lookup("B-2")).thenReturn(new InventoryResponse("B-2", 0));
        String unusedOutcome = finish(unusedSession); // "UnnecessaryStubbingException"

        // 3. The escape hatch: lenient strictness tolerates the identical unused stub.
        MockitoSession lenientSession =
                Mockito.mockitoSession().strictness(Strictness.LENIENT).startMocking();
        InventoryClient lenientClient = mock(InventoryClient.class);
        when(lenientClient.lookup("C-3")).thenReturn(new InventoryResponse("C-3", 1));
        String lenientOutcome = finish(lenientSession); // "no failure"

        Strictness extensionDefault = Strictness.STRICT_STUBS; // what MockitoExtension uses

        System.out.println("Can fulfil: " + canFulfil); // Can fulfil: true
        System.out.println("Used stub: " + usedOutcome); // Used stub: no failure
        System.out.println("Unused: " + unusedOutcome); // Unused: UnnecessaryStubbingException
        System.out.println("Lenient stub: " + lenientOutcome); // Lenient stub: no failure
        System.out.println("Default: " + extensionDefault); // Default: STRICT_STUBS
    }

    /** Finishes a session, reporting the failure a strict session raises instead of throwing it. */
    private static String finish(MockitoSession session) {
        try {
            session.finishMocking();
            return "no failure";
        } catch (UnnecessaryStubbingException e) {
            return e.getClass().getSimpleName();
        }
    }
}
