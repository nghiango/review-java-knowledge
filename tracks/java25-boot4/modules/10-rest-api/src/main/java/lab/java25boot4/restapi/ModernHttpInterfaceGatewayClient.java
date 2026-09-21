package lab.java25boot4.restapi;

import java.net.URI;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** Modern declarative HTTP interface client demonstration in Spring Framework 7 / Spring Boot 4. */
public class ModernHttpInterfaceGatewayClient {

    @HttpExchange(
            url = "/api/v2/payments",
            accept = MediaType.APPLICATION_JSON_VALUE,
            contentType = MediaType.APPLICATION_JSON_VALUE)
    public interface PaymentExchange {

        @PostExchange("/charges")
        ChargeResponse createCharge(@RequestBody ChargeRequest request);

        @GetExchange("/charges/{chargeId}")
        ChargeResponse getCharge(@PathVariable String chargeId);
    }

    public record ChargeRequest(String orderId, long amountCents, String currency) {}

    public record ChargeResponse(String chargeId, String status, String transactionRef) {}

    /** Domain exception preserving downstream RFC 9457 ProblemDetail diagnostics. */
    public static class DownstreamProblemException extends RuntimeException {
        private final ProblemDetail problemDetail;

        public DownstreamProblemException(ProblemDetail problemDetail) {
            super(
                    "Downstream service reported problem: "
                            + problemDetail.getTitle()
                            + " - "
                            + problemDetail.getDetail());
            this.problemDetail = problemDetail;
        }

        public ProblemDetail getProblemDetail() {
            return problemDetail;
        }
    }

    /** Factory method creating a bounded, timeout-safe declarative HTTP interface proxy. */
    public static PaymentExchange createClient(
            String baseUrl, Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());

        RestClient restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .requestFactory(requestFactory)
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .defaultStatusHandler(
                                HttpStatusCode::isError,
                                (request, response) -> {
                                    ProblemDetail problem =
                                            ProblemDetail.forStatusAndDetail(
                                                    response.getStatusCode(),
                                                    "Downstream call to "
                                                            + request.getURI().getPath()
                                                            + " failed with status "
                                                            + response.getStatusCode());
                                    problem.setTitle("Downstream Payment Provider Failure");
                                    problem.setType(
                                            URI.create(
                                                    "https://api.example.com/errors/payment-downstream-error"));
                                    problem.setProperty(
                                            "downstreamStatus", response.getStatusCode().value());
                                    throw new DownstreamProblemException(problem);
                                })
                        .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(PaymentExchange.class);
    }
}
