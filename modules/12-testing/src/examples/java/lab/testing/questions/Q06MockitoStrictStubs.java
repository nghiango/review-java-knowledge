package lab.testing.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lab.testing.fulfilment.InventoryClient;
import lab.testing.fulfilment.InventoryResponse;
import lab.testing.fulfilment.OrderFulfilmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.exceptions.misusing.UnnecessaryStubbingException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Q06: Mockito strict stubs — what the strictness level actually checks.
 *
 * <p>{@code @ExtendWith(MockitoExtension.class)} opens a {@link MockitoSession} before each test
 * with {@link Strictness#STRICT_STUBS} and calls {@code finishMocking()} after it, so a stub that
 * was never used fails the test with an {@link UnnecessaryStubbingException} — even when every
 * assertion in it passed. An argument mismatch is reported immediately too, instead of quietly
 * returning {@code null}. The nested classes are the three shapes a test can have; {@code main}
 * drives the session the extension opens, so the mechanism is visible and the outcome is printed
 * rather than reported by the engine.
 */
public class Q06MockitoStrictStubs {

    /** Every stub is used: the session finishes clean. */
    @ExtendWith(MockitoExtension.class)
    static class UsedStubTest {

        @Mock private InventoryClient inventoryClient;

        @Test
        @DisplayName("a stub the test uses leaves the session clean")
        void canFulfil_availableStock_returnsTrue() {
            when(inventoryClient.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 3));

            assertThat(new OrderFulfilmentService(inventoryClient).canFulfil("A-1", 3)).isTrue();
        }
    }

    /** The failure strict stubs exist for: the assertion passes and the test fails anyway. */
    @ExtendWith(MockitoExtension.class)
    static class UnusedStubTest {

        @Mock private InventoryClient inventoryClient;

        @Test
        @DisplayName("a stub nobody uses fails the test after its assertions passed")
        void canFulfil_unusedStub_raisesUnnecessaryStubbing() {
            when(inventoryClient.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 3));
            when(inventoryClient.lookup("B-2")).thenReturn(new InventoryResponse("B-2", 0));

            assertThat(new OrderFulfilmentService(inventoryClient).canFulfil("A-1", 3)).isTrue();
            // finishMocking() then raises UnnecessaryStubbingException for the unused B-2 stub.
        }
    }

    /** The escape hatch, for a stub that is genuinely shared setup rather than dead weight. */
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    static class LenientStubTest {

        @Mock private InventoryClient inventoryClient;

        @Test
        @DisplayName("lenient strictness tolerates the same unused stub")
        void canFulfil_unusedStub_isTolerated() {
            when(inventoryClient.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 3));
            when(inventoryClient.lookup("B-2")).thenReturn(new InventoryResponse("B-2", 0));

            assertThat(new OrderFulfilmentService(inventoryClient).canFulfil("A-1", 3)).isTrue();
        }
    }

    public static void main(String[] args) {
        // 1. Every stub is used: the strict session finishes clean.
        MockitoSession usedSession =
                Mockito.mockitoSession().strictness(Strictness.STRICT_STUBS).startMocking();
        InventoryClient usedClient = mock(InventoryClient.class);
        when(usedClient.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 3));
        boolean canFulfil = new OrderFulfilmentService(usedClient).canFulfil("A-1", 3); // true
        String usedOutcome = finish(usedSession); // "no failure"

        // 2. One of the two stubs is never used: the session reports it instead of ignoring it.
        MockitoSession unusedSession =
                Mockito.mockitoSession().strictness(Strictness.STRICT_STUBS).startMocking();
        InventoryClient client = mock(InventoryClient.class);
        when(client.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 3));
        when(client.lookup("B-2")).thenReturn(new InventoryResponse("B-2", 0)); // never used
        boolean canFulfilA1 = new OrderFulfilmentService(client).canFulfil("A-1", 3); // true
        long unusedStubbings = unusedStubbings(client); // 1
        String unusedOutcome = finish(unusedSession); // "UnnecessaryStubbingException"

        // 3. The same two stubs under LENIENT strictness are tolerated.
        MockitoSession lenientSession =
                Mockito.mockitoSession().strictness(Strictness.LENIENT).startMocking();
        InventoryClient lenientClient = mock(InventoryClient.class);
        when(lenientClient.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 3));
        when(lenientClient.lookup("B-2")).thenReturn(new InventoryResponse("B-2", 0));
        boolean lenientCanFulfil =
                new OrderFulfilmentService(lenientClient).canFulfil("A-1", 3); // true
        String lenientOutcome = finish(lenientSession); // "no failure"

        boolean everyCallSucceeded = canFulfil && canFulfilA1 && lenientCanFulfil; // true
        String extension = MockitoExtension.class.getSimpleName(); // "MockitoExtension"
        Strictness extensionDefault = Strictness.STRICT_STUBS; // what the extension opens with

        System.out.println("Extension: " + extension); // Extension: MockitoExtension
        System.out.println("Default: " + extensionDefault); // Default: STRICT_STUBS
        System.out.println("Used stub: " + usedOutcome); // Used stub: no failure
        System.out.println("Unused stubbings: " + unusedStubbings); // Unused stubbings: 1
        System.out.println("Unused: " + unusedOutcome); // Unused: UnnecessaryStubbingException
        System.out.println("Lenient stub: " + lenientOutcome); // Lenient stub: no failure
        System.out.println(
                "Every call succeeded: " + everyCallSucceeded); // Every call succeeded: true
    }

    /** Finishes a session, reporting the failure a strict session raises instead of throwing it. */
    private static String finish(MockitoSession session) {
        try {
            session.finishMocking();
            return "no failure";
        } catch (UnnecessaryStubbingException e) {
            return e.getClass().getSimpleName(); // "UnnecessaryStubbingException"
        }
    }

    /** Counts the stubs of {@code client} that no test code has used. */
    private static long unusedStubbings(InventoryClient client) {
        return Mockito.mockingDetails(client).getStubbings().stream()
                .filter(stubbing -> !stubbing.wasUsed())
                .count();
    }
}
