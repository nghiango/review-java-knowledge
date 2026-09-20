package lab.springboot.questions;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Q11ConditionEvaluationReportExample {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.refresh();

        ConditionEvaluationReport report = ConditionEvaluationReport.get(context.getBeanFactory());
        report.recordConditionEvaluation(
                "sampleAutoConfig", null, ConditionOutcome.match("Class on classpath"));

        boolean hasEvaluations = !report.getConditionAndOutcomesBySource().isEmpty(); // true
        String matchOutcome =
                report.getConditionAndOutcomesBySource()
                        .get("sampleAutoConfig")
                        .iterator()
                        .next()
                        .getOutcome()
                        .getMessage(); // "Class on classpath"

        System.out.println(
                "Report has evaluations: " + hasEvaluations + ", outcome: " + matchOutcome);
        context.close();
    }
}
