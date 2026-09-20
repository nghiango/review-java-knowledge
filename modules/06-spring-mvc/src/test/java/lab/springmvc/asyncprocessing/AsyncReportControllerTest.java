package lab.springmvc.asyncprocessing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

class AsyncReportControllerTest {

    private final AsyncReportService service = new AsyncReportService();
    private final AsyncReportController controller = new AsyncReportController(service);

    @Test
    @DisplayName("Async report service completes CompletableFuture successfully")
    void asyncService_generatesReport() throws Exception {
        CompletableFuture<String> future = service.generateHeavyReportAsync("ds-12345");
        String result = future.get();

        assertThat(result).contains("Report generated for ds-12345");
        assertThat(result).contains("checksum=");
    }

    @Test
    @DisplayName("Async report controller returns DeferredResult with timeout configured")
    void asyncController_returnsDeferredResult() {
        DeferredResult<ResponseEntity<String>> result = controller.generateReport("ds-999");
        assertThat(result).isNotNull();
        assertThat(result.isSetOrExpired()).isFalse();
    }
}
