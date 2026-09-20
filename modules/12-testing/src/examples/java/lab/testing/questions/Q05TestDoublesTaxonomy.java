package lab.testing.questions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import lab.testing.fulfilment.InventoryClient;
import lab.testing.fulfilment.InventoryResponse;
import lab.testing.fulfilment.OrderFulfilmentService;
import lab.testing.pricing.Money;
import lab.testing.pricing.PaymentGateway;
import org.mockito.Mockito;

/**
 * Q05: The test-double taxonomy — dummy, stub, spy, mock, fake.
 *
 * <p>The names describe *roles in a test*, not the framework that produces them: a dummy fills a
 * parameter, a stub answers with canned data, a spy wraps a real object and records calls, a mock
 * is a stub the test also verifies, and a fake is a hand-written implementation with real
 * behaviour. Only a fake has behaviour the production code actually exercises, which is why a fake
 * is the right double for a repository and a mock is the wrong one.
 */
public class Q05TestDoublesTaxonomy {

    /** A fake: a real, working implementation with no configuration for the test to supply. */
    static final class InMemoryGateway implements PaymentGateway {

        private final List<Money> charges = new ArrayList<>();

        @Override
        public void charge(Money amount) {
            charges.add(amount);
        }

        List<Money> charges() {
            return List.copyOf(charges);
        }
    }

    public static void main(String[] args) {
        // Dummy: exists only to fill a parameter; never stubbed and never verified.
        InventoryClient dummy = mock(InventoryClient.class); // an unstubbed mock answers null

        // Stub: answers a call with a canned value; the test only cares about the answer.
        InventoryClient stub = mock(InventoryClient.class);
        when(stub.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 3));
        boolean stubbedCanFulfil = new OrderFulfilmentService(stub).canFulfil("A-1", 3); // true

        // Mock: a stub the test also verifies interactions on.
        InventoryClient mocked = mock(InventoryClient.class);
        when(mocked.lookup("A-1")).thenReturn(new InventoryResponse("A-1", 5));
        boolean mockedCanFulfil = new OrderFulfilmentService(mocked).canFulfil("A-1", 5); // true
        verify(mocked).lookup("A-1"); // the interaction the test asserts on

        // Spy: wraps a real object, runs its real methods and records the interactions.
        List<Money> realList = new ArrayList<>();
        List<Money> spyList = spy(realList);
        spyList.add(new Money(100)); // runs the real ArrayList.add
        int spySize = spyList.size(); // 1

        // Fake: hand-written, no framework, real behaviour.
        InMemoryGateway fake = new InMemoryGateway();
        fake.charge(new Money(250));
        int fakeCharges = fake.charges().size(); // 1

        boolean stubIsAMock = Mockito.mockingDetails(stub).isMock(); // true
        boolean spyIsASpy = Mockito.mockingDetails(spyList).isSpy(); // true
        boolean fakeIsAMock = Mockito.mockingDetails(fake).isMock(); // false
        int dummyInvocations = Mockito.mockingDetails(dummy).getInvocations().size(); // 0

        System.out.println("Stubbed can fulfil: " + stubbedCanFulfil); // Stubbed can fulfil: true
        System.out.println("Mocked can fulfil: " + mockedCanFulfil); // Mocked can fulfil: true
        System.out.println("Spy size: " + spySize); // Spy size: 1
        System.out.println("Fake charges: " + fakeCharges); // Fake charges: 1
        System.out.println("Stub is a mock: " + stubIsAMock); // Stub is a mock: true
        System.out.println("Spy is a spy: " + spyIsASpy); // Spy is a spy: true
        System.out.println("Fake is a mock: " + fakeIsAMock); // Fake is a mock: false
        System.out.println("Dummy invocations: " + dummyInvocations); // Dummy invocations: 0
    }
}
