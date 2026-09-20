package lab.restapi.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@Validated
public class SafeCatalogController {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private final CatalogItemRepository repository;

    public SafeCatalogController(CatalogItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CatalogItem>> getCatalog(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_SIZE) int size) {

        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);
        int safePage = Math.max(0, page);

        PagedResponse<CatalogItem> response = repository.findAll(category, safePage, safeSize);
        return ResponseEntity.ok(response);
    }
}
