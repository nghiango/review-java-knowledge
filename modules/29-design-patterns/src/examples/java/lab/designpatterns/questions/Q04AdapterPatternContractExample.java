package lab.designpatterns.questions;

/**
 * Q04: Adapter Pattern. Demonstrates adapting a legacy/third-party API into an internal application
 * interface.
 */
public class Q04AdapterPatternContractExample {

    // Internal Target Interface
    public interface ModernPaymentGateway {
        boolean pay(String account, double amount);
    }

    // Third-party Adaptee (incompatible method signature)
    public static class LegacyVendorPaymentClient {
        public int makeDirectDebit(String acctNumber, long cents) {
            return (cents > 0) ? 200 : 400; // Returns HTTP status
        }
    }

    // Adapter
    public static class VendorPaymentGatewayAdapter implements ModernPaymentGateway {
        private final LegacyVendorPaymentClient vendorClient;

        public VendorPaymentGatewayAdapter(LegacyVendorPaymentClient vendorClient) {
            this.vendorClient = vendorClient;
        }

        @Override
        public boolean pay(String account, double amount) {
            long cents = Math.round(amount * 100);
            int status = vendorClient.makeDirectDebit(account, cents);
            return status == 200;
        }
    }

    public static void main(String[] args) {
        LegacyVendorPaymentClient vendor = new LegacyVendorPaymentClient();
        ModernPaymentGateway gateway = new VendorPaymentGatewayAdapter(vendor);

        boolean success =
                gateway.pay(
                        "ACC-987", 49.99); // true (adapter translated dollar double to cents long)

        System.out.println("Q04 adapterSuccess: " + success);
    }
}
