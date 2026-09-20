package lab.springboot.questions;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class Q19CustomFailureAnalyzerExample {

    static class PortAlreadyInUseException extends RuntimeException {
        private final int port;

        public PortAlreadyInUseException(int port) {
            super("Port " + port + " is already occupied by another process");
            this.port = port;
        }

        public int getPort() {
            return port;
        }
    }

    static class PortInUseFailureAnalyzer
            extends AbstractFailureAnalyzer<PortAlreadyInUseException> {
        @Override
        protected FailureAnalysis analyze(Throwable rootFailure, PortAlreadyInUseException cause) {
            String description =
                    "Web server failed to start. Port " + cause.getPort() + " was already in use.";
            String action =
                    "Identify and stop the process listening on port "
                            + cause.getPort()
                            + " or configure server.port="
                            + (cause.getPort() + 1);
            return new FailureAnalysis(description, action, cause);
        }
    }

    public static void main(String[] args) {
        PortInUseFailureAnalyzer analyzer = new PortInUseFailureAnalyzer();
        PortAlreadyInUseException ex = new PortAlreadyInUseException(8080);
        FailureAnalysis analysis = analyzer.analyze(ex, ex);

        String description =
                analysis.getDescription(); // "Web server failed to start. Port 8080 was already in
        // use."
        String action =
                analysis.getAction(); // "Identify and stop the process listening on port 8080 or
        // configure server.port=8081"
        boolean hasAction = action.contains("server.port"); // true

        System.out.println(
                "Failure description: "
                        + description
                        + ", action: "
                        + action
                        + ", actionable: "
                        + hasAction);
    }
}
