package lab.jpahibernate.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q27JoinFetchPaginationHazardExample {
    private Q27JoinFetchPaginationHazardExample() {}

    public record OrderSummary(Long id, int itemCount) {}

    // Problem Demonstration:
    // When executing `@Query("SELECT o FROM Order o JOIN FETCH o.items") Page<Order> findAllWithItems(Pageable p)`
    // The SQL join multiplies 100 Orders with 10 items each into 1,000 SQL rows.
    // Database LIMIT 20 OFFSET 0 would return only 2 Orders (with 10 items each = 20 rows), NOT 20 distinct Orders!
    // Therefore, Hibernate emits HHH000104 and loads all 1,000 rows into JVM memory to paginate in heap memory.
    public static class PaginationHazardSimulator {
        public static String explainHibernateWarning() {
            return "HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!";
        }

        // Solution 1: Two-Phase ID Pagination
        // Phase 1: Query pageable distinct IDs using database LIMIT/OFFSET:
        public static List<Long> queryPageOfIds(int page, int size) {
            // SELECT o.id FROM Order o ORDER BY o.id DESC LIMIT :size OFFSET :offset
            List<Long> pagedIds = new ArrayList<>();
            for (long i = 1; i <= size; i++) {
                pagedIds.add(i);
            }
            return pagedIds; // Exactly 20 distinct Order IDs
        }

        // Phase 2: Fetch Orders and items using IN clause without pagination:
        public static String buildSecondPhaseQuery(List<Long> ids) {
            // SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o.id IN (:ids)
            return "SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o.id IN (" + ids.size() + " ids)";
        }
    }

    public static void main(String[] args) {
        String warning = PaginationHazardSimulator.explainHibernateWarning();
        boolean warnsInMemory = warning.contains("applying in memory"); // true

        List<Long> ids = PaginationHazardSimulator.queryPageOfIds(0, 20);
        int pagedOrders = ids.size(); // 20 distinct orders!

        String query = PaginationHazardSimulator.buildSecondPhaseQuery(ids);
        boolean cleanFetch = query.contains("WHERE o.id IN (20 ids)"); // true
    }
}
