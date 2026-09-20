package lab.distributeddata.questions;

public class Q14TccTryConfirmCancelPatternExample {

    enum TccPhase {
        TRY,
        CONFIRM,
        CANCEL
    }

    record TccAction(String businessResource, TccPhase phase, boolean isReserved) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // TCC (Try-Confirm-Cancel) reserves resources explicitly in business state:
        // Try: check availability and reserve balance (e.g. balance = 100, reserved = 20)
        // Confirm: commit the reservation permanently
        // Cancel: release the reservation back to available balance
        TccAction tryPhase = new TccAction("AccountBalance", TccPhase.TRY, true);
        TccAction cancelPhase = new TccAction("AccountBalance", TccPhase.CANCEL, false);

        boolean tryReserves = tryPhase.isReserved(); // true
        boolean cancelReleases = !cancelPhase.isReserved(); // true

        System.out.println(
                "TCC Try reserves: " + tryReserves + ", Cancel releases: " + cancelReleases);
    }
}
