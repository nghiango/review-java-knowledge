package lab.jpahibernate.jsonrecursion;

import java.util.List;

public record AuthorResponseDto(Long id, String name, List<BookSummaryDto> books) {
    public record BookSummaryDto(Long id, String title, String isbn) {}
}
