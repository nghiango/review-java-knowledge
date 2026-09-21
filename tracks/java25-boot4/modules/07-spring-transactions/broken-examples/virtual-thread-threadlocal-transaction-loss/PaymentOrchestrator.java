package lab.java25boot4.springtransactions.broken.virtualthreadcontext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentOrchestrator {

    private static final ThreadLocal<String> CURRENT_TX_CORRELATION = new ThreadLocal<>();
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Transactional
    public void executePayment(String paymentId, double amount, Runnable onCompleteCallback) {
        CURRENT_TX_CORRELATION.set(paymentId);

        // Offloading callback to asynchronous virtual thread
        virtualExecutor.submit(() -> {
            // Memory / context issue: ThreadLocal is NOT visible in virtual thread
            String correlation = CURRENT_TX_CORRELATION.get();
            if (correlation == null) {
                System.err.println("Lost transaction correlation in virtual worker!");
            }
            onCompleteCallback.run();
        });

        // Missing cleanup: CURRENT_TX_CORRELATION.remove() never called
    }
}
