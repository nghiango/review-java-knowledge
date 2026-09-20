package lab.webflux.flatmapconcurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class CorrectNotificationBatchSenderTest {

    @Test
    @DisplayName("Should isolate errors per message and complete batch execution")
    void sendBatch_isolatesErrorsAndCompletes() {
        CorrectNotificationBatchSender sender =
                new CorrectNotificationBatchSender(WebClient.builder(), "http://127.0.0.1:54321");

        List<CorrectNotificationBatchSender.NotificationMessage> messages =
                List.of(
                        new CorrectNotificationBatchSender.NotificationMessage(
                                "u-1", "Hello", "World"),
                        new CorrectNotificationBatchSender.NotificationMessage(
                                "u-2", "Alert", "Test"),
                        new CorrectNotificationBatchSender.NotificationMessage(
                                "u-3", "Notice", "Info"));

        Flux<CorrectNotificationBatchSender.NotificationResult> resultFlux =
                sender.sendBatch(messages, 2);

        StepVerifier.create(resultFlux)
                .assertNext(
                        res -> {
                            assertThat(res.userId()).isEqualTo("u-1");
                            assertThat(res.delivered()).isFalse();
                        })
                .assertNext(
                        res -> {
                            assertThat(res.userId()).isEqualTo("u-2");
                            assertThat(res.delivered()).isFalse();
                        })
                .assertNext(
                        res -> {
                            assertThat(res.userId()).isEqualTo("u-3");
                            assertThat(res.delivered()).isFalse();
                        })
                .verifyComplete();
    }
}
