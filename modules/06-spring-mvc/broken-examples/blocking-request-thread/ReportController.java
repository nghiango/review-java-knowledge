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

    @GetMapping("/heavy")
    public String generateReport(@RequestParam("datasetId") String datasetId) {
        return reportService.generateHeavyReport(datasetId);
    }
}
