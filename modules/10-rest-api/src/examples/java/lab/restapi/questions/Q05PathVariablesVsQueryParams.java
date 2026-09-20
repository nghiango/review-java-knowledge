package lab.restapi.questions;

public class Q05PathVariablesVsQueryParams {

    public static void main(String[] args) {
        // Path Variables: Resource identification and hierarchy
        String itemUri = "/api/departments/engineering/employees/42";
        boolean identifiesResource = itemUri.matches(".*/employees/\\d+"); // true

        // Query Parameters: Optional filtering, sorting, pagination, projections
        String searchUri =
                "/api/employees?department=engineering&role=lead&page=0&size=20&sort=name,asc";
        boolean hasFilters = searchUri.contains("department=engineering"); // true
        boolean hasPagination = searchUri.contains("page=0&size=20"); // true

        System.out.println(
                "Path variable identifies exact resource: "
                        + identifiesResource); // Path variable identifies exact resource: true
        System.out.println(
                "Query params contain filter/pagination: "
                        + (hasFilters
                                && hasPagination)); // Query params contain filter/pagination: true
    }
}
