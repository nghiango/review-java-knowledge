package lab.designpatterns.questions;

/**
 * Q10: State Pattern Transition Invariants. Demonstrates state-specific behavior where invalid
 * transitions are rejected.
 */
public class Q10StatePatternTransitionExample {

    public interface State {
        String handlePayment();

        String handleShipment();
    }

    public static class DraftState implements State {
        @Override
        public String handlePayment() {
            return "PAID_SUCCESS";
        }

        @Override
        public String handleShipment() {
            throw new IllegalStateException("Cannot ship draft order");
        }
    }

    public static class PaidState implements State {
        @Override
        public String handlePayment() {
            throw new IllegalStateException("Already paid");
        }

        @Override
        public String handleShipment() {
            return "DISPATCHED";
        }
    }

    public static void main(String[] args) {
        State draft = new DraftState();
        String paymentResult = draft.handlePayment(); // "PAID_SUCCESS"

        boolean threwOnUnpaidShip = false;
        try {
            draft.handleShipment();
        } catch (IllegalStateException e) {
            threwOnUnpaidShip = true; // true
        }

        System.out.println("Q10 paid: " + paymentResult + ", caught: " + threwOnUnpaidShip);
    }
}
