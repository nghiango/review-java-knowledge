package lab.concurrency.broken.lockordering;

public class AuditNotificationClient {
    public void sendNotification(String message) {
        try {
            // Simulated external HTTP/Webhook notification
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
