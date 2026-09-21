package lab.java25boot4.springmvc.questions;

public class Q05VersionResolutionStrategyExample {

    enum VersionStrategy {
        HEADER("X-API-Version"),
        QUERY_PARAM("api-version"),
        MEDIA_TYPE("application/vnd.company.app-v1+json");

        private final String key;

        VersionStrategy(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static void main(String[] args) {
        System.out.println(VersionStrategy.HEADER.getKey()); // X-API-Version
        System.out.println(VersionStrategy.QUERY_PARAM.getKey()); // api-version
    }
}
