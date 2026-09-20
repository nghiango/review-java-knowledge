package lab.springboot.autoconfigoverride;

public class ExternalPaymentClient {

    private final String baseUrl;
    private final boolean metricsEnabled;
    private final boolean tracingEnabled;

    public ExternalPaymentClient(String baseUrl, boolean metricsEnabled, boolean tracingEnabled) {
        this.baseUrl = baseUrl;
        this.metricsEnabled = metricsEnabled;
        this.tracingEnabled = tracingEnabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public boolean isTracingEnabled() {
        return tracingEnabled;
    }
}
