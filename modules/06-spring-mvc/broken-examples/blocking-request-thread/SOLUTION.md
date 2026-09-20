# Solution: Blocking Long Work on Request Thread Without Timeout

## Annotated Code

### `ReportController.java`

```java
package lab.springmvc.broken.asyncprocessing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportGenerationService reportService;

    public ReportController(ReportGenerationService reportService) {
        this.reportService = reportService;
    }

    // Performance issue: Synchronously executing heavy CPU/IO processing directly holds the Tomcat HTTP worker thread
    // Reliability issue: Lacks explicit timeout handling; under high concurrency, Tomcat worker thread pool (default 200) is exhausted
    @GetMapping("/heavy")
    public String generateReport(@RequestParam("datasetId") String datasetId) {
        return reportService.generateHeavyReport(datasetId);
    }
}
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `ReportController.java:20` | `Performance` | Blocking HTTP request thread | Ties up Tomcat container worker threads during long-running computation. |
| `ReportController.java:20` | `Reliability` | Missing timeout protection | Slow backend operations hang client requests indefinitely, causing thread pool starvation. |

## Correct implementation

- Package: `lab.springmvc.asyncprocessing`
- Production reference: `AsyncReportController.java`, `AsyncReportService.java`
- Fix: Leverage Spring MVC Async via `DeferredResult<ResponseEntity<String>>` or `CompletableFuture<String>` executed on an isolated dedicated `ThreadPoolTaskExecutor`. Set explicit timeouts (`onTimeout`) to return `503 Service Unavailable` or `504 Gateway Timeout` without starving the web container thread pool.
