package lab.springcore.prototypescope;

import java.time.Instant;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ExecutionContext {
    private String requestId;
    private Instant startTime;

    public void initialize(String requestId) {
        this.requestId = requestId;
        this.startTime = Instant.now();
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getStartTime() {
        return startTime;
    }
}
