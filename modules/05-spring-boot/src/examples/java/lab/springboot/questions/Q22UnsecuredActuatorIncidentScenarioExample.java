package lab.springboot.questions;

import java.util.Set;

public class Q22UnsecuredActuatorIncidentScenarioExample {

    record IncidentReport(
            String incidentId,
            String rootCause,
            Set<String> compromisedEndpoints,
            String remediation) {}

    public static void main(String[] args) {
        // Incident Simulation: Leaked credentials via /actuator/env
        IncidentReport report =
                new IncidentReport(
                        "INC-8492",
                        "Wildcard Actuator exposure (include: '*') allowed unauthenticated access to /actuator/env with show-values: always",
                        Set.of("/actuator/env", "/actuator/heapdump"),
                        "Restrict include to health and info, bind management port to internal subnet, and set show-values to never");

        boolean involvesEnv = report.compromisedEndpoints().contains("/actuator/env"); // true
        boolean hasRemediation = report.remediation().contains("internal subnet"); // true

        System.out.println(
                "Incident: "
                        + report.incidentId()
                        + ", env leaked: "
                        + involvesEnv
                        + ", remediated: "
                        + hasRemediation);
    }
}
