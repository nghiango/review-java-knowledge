package lab.java25boot4.restapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ModernHttpInterfaceGatewayClientTest {

    @Test
    @DisplayName("createClient instantiates a valid HttpExchange proxy without error")
    void shouldCreateDeclarativeHttpInterfaceProxy() {
        var client =
                ModernHttpInterfaceGatewayClient.createClient(
                        "https://httpbin.org", Duration.ofMillis(1000), Duration.ofMillis(1500));

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("DownstreamProblemException preserves structured ProblemDetail attributes")
    void downstreamProblemExceptionShouldPreserveProblemDetail() {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_GATEWAY, "Payment network rejected transaction");
        problem.setTitle("Payment Provider Error");
        problem.setType(URI.create("https://api.example.com/errors/gateway-rejected"));
        problem.setProperty("gatewayCode", "CARD_DECLINED_TIMEOUT");

        var ex = new ModernHttpInterfaceGatewayClient.DownstreamProblemException(problem);

        assertThat(ex.getProblemDetail().getStatus()).isEqualTo(502);
        assertThat(ex.getProblemDetail().getTitle()).isEqualTo("Payment Provider Error");
        assertThat(ex.getProblemDetail().getProperties())
                .containsEntry("gatewayCode", "CARD_DECLINED_TIMEOUT");
        assertThat(ex.getMessage()).contains("Payment Provider Error");
    }
}
