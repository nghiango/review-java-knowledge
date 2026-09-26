package lab.observability.questions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates Micrometer ObservationFilter for appending global tags/key-values
 * (such as environment, region, tenant) or redacting high-cardinality values across all observations.
 */
public class Q24ObservationFilterKeyValuesCustomizationExample {

    public static class EnvironmentTaggingObservationFilter implements ObservationFilter {
        private final String environment;

        public EnvironmentTaggingObservationFilter(String environment) {
            this.environment = environment;
        }

        @Override
        public Observation.Context map(Observation.Context context) {
            return context.addLowCardinalityKeyValue(KeyValue.of("env", environment));
        }
    }

    public static void main(String[] args) {
        Observation.Context context = new Observation.Context();
        context.setName("http.server.requests");

        ObservationFilter filter = new EnvironmentTaggingObservationFilter("production");
        Observation.Context enrichedContext = filter.map(context);

        List<KeyValue> keyValues = new ArrayList<>();
        enrichedContext.getLowCardinalityKeyValues().forEach(keyValues::add);

        boolean hasEnvTag = keyValues.stream().anyMatch(kv -> "env".equals(kv.getKey()) && "production".equals(kv.getValue())); // true
        System.out.println("ObservationContext enriched with env: " + hasEnvTag);
    }
}
