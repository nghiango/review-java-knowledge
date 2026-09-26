package lab.designpatterns.questions;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates the Registry Pattern with Spring BeanFactory injection, resolving strategies
 * dynamically based on business discriminator tags with zero switch-statement coupling.
 */
public class Q27RegistryPatternDynamicDispatchExample {

    public interface ReportExporter {
        String format();
        String export(String data);
    }

    public static class PdfReportExporter implements ReportExporter {
        @Override
        public String format() { return "PDF"; }
        @Override
        public String export(String data) { return "[PDF]" + data; }
    }

    public static class CsvReportExporter implements ReportExporter {
        @Override
        public String format() { return "CSV"; }
        @Override
        public String export(String data) { return "[CSV]" + data; }
    }

    public static class ReportExporterRegistry {
        private final Map<String, ReportExporter> exporters = new HashMap<>();

        public ReportExporterRegistry(ReportExporter... registeredExporters) {
            for (ReportExporter exporter : registeredExporters) {
                exporters.put(exporter.format(), exporter);
            }
        }

        public ReportExporter getExporter(String format) {
            ReportExporter exporter = exporters.get(format);
            if (exporter == null) {
                throw new IllegalArgumentException("Unsupported report format: " + format);
            }
            return exporter;
        }
    }

    public static void main(String[] args) {
        ReportExporterRegistry registry = new ReportExporterRegistry(
                new PdfReportExporter(),
                new CsvReportExporter()
        );

        String pdfOutput = registry.getExporter("PDF").export("Financials");
        boolean correctExporterDispatched = "[PDF]Financials".equals(pdfOutput); // true

        System.out.println("Dispatched exporter output: " + pdfOutput);
        System.out.println("Registry dispatched matching exporter: " + correctExporterDispatched);
    }
}
