package lab.restapi.pagination;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogItemRepository {

    private final List<CatalogItem> database = new ArrayList<>();

    public CatalogItemRepository() {
        for (int i = 1; i <= 150; i++) {
            database.add(
                    new CatalogItem(
                            UUID.randomUUID(),
                            "SKU-" + String.format("%04d", i),
                            "Item " + i,
                            i % 2 == 0 ? "Electronics" : "Books",
                            BigDecimal.valueOf(10 + i)));
        }
    }

    public PagedResponse<CatalogItem> findAll(String category, int page, int size) {
        var filtered =
                database.stream()
                        .filter(
                                item ->
                                        category == null
                                                || item.category().equalsIgnoreCase(category))
                        .toList();

        long totalElements = filtered.size();
        int fromIndex = Math.min(page * size, (int) totalElements);
        int toIndex = Math.min(fromIndex + size, (int) totalElements);

        List<CatalogItem> content = filtered.subList(fromIndex, toIndex);
        return PagedResponse.of(content, page, size, totalElements);
    }
}
