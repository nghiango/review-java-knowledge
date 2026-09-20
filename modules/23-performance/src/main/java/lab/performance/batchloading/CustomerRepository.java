package lab.performance.batchloading;

import java.util.Collection;
import java.util.Map;

public interface CustomerRepository {
    Map<Long, Customer> findAllById(Collection<Long> ids);
}
