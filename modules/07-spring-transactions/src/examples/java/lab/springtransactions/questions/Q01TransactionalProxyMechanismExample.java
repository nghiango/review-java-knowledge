package lab.springtransactions.questions;

public class Q01TransactionalProxyMechanismExample {

    record InvocationContext(
            String targetMethod, boolean isProxyPassed, boolean transactionActive) {}

    public static void main(String[] args) {
        // External call passes through Spring AOP proxy -> TransactionInterceptor begins
        // transaction
        InvocationContext externalCall = new InvocationContext("placeOrder", true, true);

        // Self-invocation directly calls this.placeOrder() -> Bypasses proxy, NO transaction
        InvocationContext selfInvocation = new InvocationContext("placeOrder", false, false);

        boolean proxyStartsTx = externalCall.transactionActive(); // true
        boolean selfInvocationFailsTx = !selfInvocation.transactionActive(); // true

        System.out.println(
                "Proxy starts TX: "
                        + proxyStartsTx
                        + ", Self-invocation bypasses TX: "
                        + selfInvocationFailsTx);
    }
}
