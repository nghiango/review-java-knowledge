package lab.springcore.prototypescope;

import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ReportGenerator {

    private final ObjectProvider<ExecutionContext> contextProvider;

    public ReportGenerator(ObjectProvider<ExecutionContext> contextProvider) {
        this.contextProvider =
                Objects.requireNonNull(contextProvider, "contextProvider must not be null");
    }

    public String generateReport(String requestId) {
        // ObjectProvider obtains a fresh prototype bean instance for each method execution
        ExecutionContext context = contextProvider.getObject();
        context.initialize(requestId);
        return "Report for request: "
                + context.getRequestId()
                + " started at: "
                + context.getStartTime();
    }
}
