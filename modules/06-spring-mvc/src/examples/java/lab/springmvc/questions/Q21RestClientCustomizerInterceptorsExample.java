package lab.springmvc.questions;

import java.util.ArrayList;
import java.util.List;

public class Q21RestClientCustomizerInterceptorsExample {

    record HttpRequestContext(String url, List<String> headers) {}

    public static void main(String[] args) {
        // ClientHttpRequestInterceptor appends headers (e.g. Bearer token, correlation ID) before
        // request dispatch
        HttpRequestContext request =
                new HttpRequestContext("https://api.payments.com/v1/charge", new ArrayList<>());
        request.headers().add("Authorization: Bearer token_xyz123");
        request.headers().add("X-Correlation-Id: trace-abc-7788");

        boolean hasAuthHeader =
                request.headers().stream().anyMatch(h -> h.startsWith("Authorization:")); // true
        boolean hasTraceId =
                request.headers().stream().anyMatch(h -> h.startsWith("X-Correlation-Id:")); // true

        System.out.println("Interceptor added auth: " + hasAuthHeader + ", trace: " + hasTraceId);
    }
}
