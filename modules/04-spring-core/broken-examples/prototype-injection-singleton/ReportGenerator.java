package lab.springcore.broken.prototypescope;

import org.springframework.stereotype.Service;

@Service
public class ReportGenerator {

    // Anti-pattern: Prototype bean injected directly into a Singleton bean.
    // The prototype is instantiated only once during singleton creation and reused forever.
    private final ExecutionContext executionContext;

    public ReportGenerator(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public String generateReport(String requestId) {
        executionContext.initialize(requestId);
        // Generates report using context state
        return "Report for request: " + executionContext.getRequestId() + " started at: "
                + executionContext.getStartTime();
    }
}
