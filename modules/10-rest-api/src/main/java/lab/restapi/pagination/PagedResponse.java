package lab.restapi.pagination;

import java.util.List;

public record PagedResponse<T>(List<T> content, PageMetadata page) {

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;
        PageMetadata metadata =
                new PageMetadata(page, size, totalElements, totalPages, hasNext, hasPrevious);
        return new PagedResponse<>(content, metadata);
    }
}
