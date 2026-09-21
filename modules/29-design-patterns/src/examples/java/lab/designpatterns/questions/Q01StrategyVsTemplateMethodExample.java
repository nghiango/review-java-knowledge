package lab.designpatterns.questions;

/**
 * Q01: Strategy vs Template Method Pattern. Demonstrates composition-based Strategy vs
 * inheritance-based Template Method.
 */
public class Q01StrategyVsTemplateMethodExample {

    // 1. Strategy: Composition & Delegation
    public interface CompressionStrategy {
        String compress(String input);
    }

    public static class ZipCompressionStrategy implements CompressionStrategy {
        @Override
        public String compress(String input) {
            return "ZIP[" + input + "]";
        }
    }

    public static class FileArchiver {
        private final CompressionStrategy strategy;

        public FileArchiver(CompressionStrategy strategy) {
            this.strategy = strategy;
        }

        public String archive(String file) {
            return strategy.compress(file);
        }
    }

    // 2. Template Method: Inheritance & Invariant Skeleton
    public abstract static class ReportGenerator {
        public final String generate() {
            return formatHeader() + " | " + formatBody() + " | " + formatFooter();
        }

        protected String formatHeader() {
            return "HEADER";
        }

        protected abstract String formatBody();

        protected String formatFooter() {
            return "FOOTER";
        }
    }

    public static class FinancialReportGenerator extends ReportGenerator {
        @Override
        protected String formatBody() {
            return "BALANCE: $1000";
        }
    }

    public static void main(String[] args) {
        FileArchiver archiver = new FileArchiver(new ZipCompressionStrategy());
        String compressed = archiver.archive("data.csv"); // "ZIP[data.csv]"

        ReportGenerator report = new FinancialReportGenerator();
        String output = report.generate(); // "HEADER | BALANCE: $1000 | FOOTER"

        boolean strategyUsed = compressed.equals("ZIP[data.csv]"); // true
        boolean templateUsed = output.contains("BALANCE: $1000"); // true

        System.out.println("Q01 strategy: " + strategyUsed + ", template: " + templateUsed);
    }
}
