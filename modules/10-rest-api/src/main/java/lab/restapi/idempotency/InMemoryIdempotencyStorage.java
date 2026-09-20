package lab.restapi.idempotency;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryIdempotencyStorage implements IdempotencyStorage {

    private final Map<String, PaymentResponse> completedResponses = new ConcurrentHashMap<>();
    private final Map<String, Long> activeLocks = new ConcurrentHashMap<>();

    @Override
    public Optional<PaymentResponse> findByKey(String key) {
        return Optional.ofNullable(completedResponses.get(key));
    }

    @Override
    public boolean lock(String key) {
        return activeLocks.putIfAbsent(key, System.currentTimeMillis()) == null;
    }

    @Override
    public void save(String key, PaymentResponse response) {
        completedResponses.put(key, response);
        activeLocks.remove(key);
    }

    @Override
    public void release(String key) {
        activeLocks.remove(key);
    }
}
