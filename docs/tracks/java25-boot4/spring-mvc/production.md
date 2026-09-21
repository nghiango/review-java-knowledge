# Production: Web Operations & API Lifecycle in Spring Boot 4

!!! info "Delta from baseline"
    Baseline operations in [`docs/topics/spring-mvc/production.md`](../../../topics/spring-mvc/production.md) cover Tomcat thread pool exhaustion, HTTP caching, and CORS monitoring.
    This page covers **API lifecycle management and observability in Spring Boot 4 / Framework 7**: API version deprecation, Sunset HTTP headers, and nullness telemetry.

---

## 1. Standard API Sunsetting & Deprecation

When decommissioning an older API version (e.g. `v1.0` in favor of `v2.0`):
- Avoid abrupt 404/410 hard breaks that destabilize client applications.
- Return standard **RFC 8594 Sunset and Deprecation** HTTP response headers on older handlers:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Deprecation: true
Sunset: 2027-01-01T00:00:00Z
Link: <https://api.example.com/docs/v2-migration>; rel="successor-version"
```

### Framework Handler Interceptor for Sunsetting

```java
public class ApiSunsetInterceptor implements HandlerInterceptor {
    @Override
    public void postHandle(HttpServletRequest req, HttpServletResponse res, Object handler, ModelAndView mv) {
        if ("1.0".equals(req.getHeader("X-API-Version"))) {
            res.setHeader("Deprecation", "true");
            res.setHeader("Sunset", "2027-01-01T00:00:00Z");
        }
    }
}
```

---

## 2. Telemetry by API Version

With first-class framework API versioning, Spring MVC automatically tags Micrometer metrics with the resolved API version:

- **Metric**: `http.server.requests`
- **Tags**:
  - `uri`: `/api/orders/{id}`
  - `version`: `1.0` vs `2.0`
  - `status`: `200`

This enables direct Grafana alerting on legacy version traffic volume to safely plan final deprecation shutdowns.
