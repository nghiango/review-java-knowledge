package lab.springsecurity.questions;

public class Q13CustomOncePerRequestFilter {

    public static void main(String[] args) {
        String requestAttributeName =
                "org.springframework.web.filter.OncePerRequestFilter.FILTERED";
        int totalExecutions = 0;

        // Simulation of filter execution logic
        boolean alreadyFiltered = false;
        if (!alreadyFiltered) {
            alreadyFiltered = true;
            totalExecutions++;
        }

        // Secondary dispatch in same request cycle
        if (!alreadyFiltered) {
            totalExecutions++;
        }

        boolean executedExactlyOnce = (totalExecutions == 1); // true

        System.out.println(
                "Filter attribute flag: " + requestAttributeName); // Filter attribute flag:
        // org.springframework.web.filter.OncePerRequestFilter.FILTERED
        System.out.println(
                "Filter execution count: " + totalExecutions); // Filter execution count: 1
        System.out.println(
                "Guaranteed exactly once execution: "
                        + executedExactlyOnce); // Guaranteed exactly once execution: true
    }
}
