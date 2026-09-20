package lab.restapi.idempotency;

import java.util.Optional;

public interface IdempotencyStorage {
    Optional<PaymentResponse> findByKey(String key);

    boolean lock(String key);

    void save(String key, PaymentResponse response);

    void release(String key);
}
