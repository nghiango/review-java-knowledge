package lab.springmvc.questions;

import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings("unused")
public final class Q26ForwardedHeaderSecurityExample {
    private Q26ForwardedHeaderSecurityExample() {}

    public static void main(String[] args) {
        // Forwarded headers (X-Forwarded-Proto, X-Forwarded-Host, X-Forwarded-Port, Forwarded):
        // Reverse proxies and API gateways terminate TLS and forward requests to internal Spring Boot services.
        // ForwardedHeaderFilter intercepts incoming requests to adapt getScheme(), getServerName(), and getServerPort()
        // based on trusted proxy headers, preventing Host header injection attacks.
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();

        // Safe URI construction using UriComponentsBuilder adapting to forwarded values:
        String proto = "https";
        String host = "api.example.com";
        int port = 443;

        String locationUri = UriComponentsBuilder.newInstance()
            .scheme(proto)
            .host(host)
            .port(port)
            .path("/api/orders/{id}")
            .buildAndExpand(42)
            .toUriString();

        boolean isHttps = locationUri.startsWith("https://api.example.com/api/orders/42"); // true
    }
}
