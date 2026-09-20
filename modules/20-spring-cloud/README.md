# Module 20 — Spring Cloud

This is a **doc module** covering Spring Cloud architecture, edge routing, declarative RPC, distributed configuration, service discovery, client-side load balancing, resilience integration, and the cloud-native evolution to Kubernetes and AWS primitives:
Spring Cloud Gateway (route predicates, WebFilter pipelines, Netty event loops, non-blocking routing, connection pools, timeouts, rate limiting), OpenFeign (declarative HTTP clients, connection/read timeouts, retryers, custom `ErrorDecoder`, circuit breaking), Spring Cloud Config Server & Client (refresh scope `@RefreshScope`, Git/Vault backend, dynamic reload hazards, encryption), Service Discovery (Eureka vs Consul vs Kubernetes DNS / CoreDNS / Envoy), Client-Side Load Balancing (Spring Cloud LoadBalancer vs Netflix Ribbon vs K8s Service `ClusterIP`), Resilience Integration (Spring Cloud CircuitBreaker / Resilience4j integration), and the Cloud-Native migration from JVM-managed discovery/routing to Kubernetes Services, Ingress / Gateway API, Service Mesh (Istio/Linkerd), and Cloud ALBs.

The canonical prose, architecture blueprints, interview Q&A, and operational incident guides live in the documentation:

👉 **[Spring Cloud Documentation](../../docs/topics/spring-cloud/index.md)**

## Broken Review Examples

This module provides 2 realistic system review targets under `broken-examples/`:

1. `gateway-unbounded-routing/` — Spring Cloud Gateway routing configuration (`application.yml`) missing connect and response timeouts, lacking request rate limiting filters, and omitting circuit breaker fallbacks, causing downstream latency spikes to exhaust Netty worker threads and connection pools.
2. `feign-missing-timeout-error-decoder/` — Declarative OpenFeign client and configuration (`InventoryClient.java`) running with unbounded infinite default timeouts, default error decoding that masks 4xx/5xx HTTP errors into generic runtime exceptions without retry classification, and unbounded retry loops.
