package lab.jpahibernate.jsonrecursion;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookCatalogService {

    private final EntityManager entityManager;

    public BookCatalogService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Maps to flat DTO projection to avoid cyclical recursion and unnecessary JSON annotations on
     * entities.
     */
    @Transactional(readOnly = true)
    public List<BookResponseDto> listBooks() {
        return entityManager
                .createQuery(
                        "SELECT new lab.jpahibernate.jsonrecursion.BookResponseDto(b.id, b.title, b.isbn, a.id, a.name) "
                                + "FROM Book b LEFT JOIN b.author a",
                        BookResponseDto.class)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public AuthorResponseDto getAuthorWithBooks(Long authorId) {
        Author author = entityManager.find(Author.class, authorId);
        if (author == null) {
            return null;
        }
        List<AuthorResponseDto.BookSummaryDto> bookDtos =
                author.getBooks().stream()
                        .map(
                                b ->
                                        new AuthorResponseDto.BookSummaryDto(
                                                b.getId(), b.getTitle(), b.getIsbn()))
                        .toList();
        return new AuthorResponseDto(author.getId(), author.getName(), bookDtos);
    }
}
