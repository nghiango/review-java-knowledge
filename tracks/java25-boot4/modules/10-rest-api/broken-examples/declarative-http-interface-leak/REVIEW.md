# Code Review: Declarative HTTP Interface Proxy Configuration & Error Propagation

## Context
A team member introduced a payment processing client using Spring Framework `@HttpExchange` declarative interfaces. The client communicates with an external downstream Payment Gateway service.

## Files Under Review
- `PaymentGatewayClient.java`
- `PaymentOrchestratorService.java`

## Review Questions
1. How are connection timeouts and read timeouts configured on the underlying `RestClient` / HTTP client? What happens to virtual or platform threads when the payment gateway experiences latency spikes or network partitions?
2. When the remote payment gateway returns an HTTP 4xx or 5xx with an RFC 9457 `ProblemDetail` payload, what happens to that detailed diagnostic information in `processPayment`?
3. How is the declarative HTTP client instantiated and registered in the Spring application context? How does this manual factory wiring compare with modern Spring Boot 4 declarative client registration?
