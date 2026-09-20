package lab.jpahibernate.jsonrecursion;

public record BookResponseDto(
        Long id, String title, String isbn, Long authorId, String authorName) {}
