package lab.designpatterns.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the Pipeline / Intercepting Filter Pattern for composable data processing stages,
 * executing ordered pre-processing and post-processing filters around a core target operation.
 */
public class Q26PipelineInterceptingFilterPatternExample {

    public interface Filter {
        String process(String request);
    }

    public static class AuthenticationFilter implements Filter {
        @Override
        public String process(String request) {
            return request + "|AUTH_OK";
        }
    }

    public static class DataSanitizationFilter implements Filter {
        @Override
        public String process(String request) {
            return request.replace("<script>", "").replace("</script>", "") + "|SANITIZED";
        }
    }

    public static class FilterPipeline {
        private final List<Filter> filters = new ArrayList<>();

        public void addFilter(Filter filter) {
            filters.add(filter);
        }

        public String execute(String initialPayload) {
            String current = initialPayload;
            for (Filter filter : filters) {
                current = filter.process(current);
            }
            return current;
        }
    }

    public static void main(String[] args) {
        FilterPipeline pipeline = new FilterPipeline();
        pipeline.addFilter(new AuthenticationFilter());
        pipeline.addFilter(new DataSanitizationFilter());

        String result = pipeline.execute("user-input<script>");
        boolean passedAllFilters = result.contains("AUTH_OK") && result.contains("SANITIZED") && !result.contains("<script>"); // true

        System.out.println("Pipeline output: " + result);
        System.out.println("Payload passed through pipeline filters: " + passedAllFilters);
    }
}
