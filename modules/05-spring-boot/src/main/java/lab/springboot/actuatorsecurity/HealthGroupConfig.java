package lab.springboot.actuatorsecurity;

import java.util.Set;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthGroupConfig {

    private final Set<String> livenessIndicators = Set.of("ping", "diskSpace");
    private final Set<String> readinessIndicators = Set.of("ping", "db", "redis");

    public Set<String> getLivenessIndicators() {
        return livenessIndicators;
    }

    public Set<String> getReadinessIndicators() {
        return readinessIndicators;
    }
}
