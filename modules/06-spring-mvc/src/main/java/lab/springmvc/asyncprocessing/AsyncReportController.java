package lab.springmvc.asyncprocessing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/api/reports")
public class AsyncReportController {

    private final AsyncReportService reportService;

    public AsyncReportController(AsyncReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/heavy")
    public DeferredResult<ResponseEntity<String>> generateReport(
            @RequestParam("datasetId") String datasetId) {
        // 5 seconds timeout window
        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>(5000L);

        deferredResult.onTimeout(
                () ->
                        deferredResult.setErrorResult(
                                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                        .body("Report generation timed out. Please retry later.")));

        reportService
                .generateHeavyReportAsync(datasetId)
                .thenAccept(result -> deferredResult.setResult(ResponseEntity.ok(result)))
                .exceptionally(
                        ex -> {
                            deferredResult.setErrorResult(
                                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                            .body("Failed to generate report"));
                            return null;
                        });

        return deferredResult;
    }
}
