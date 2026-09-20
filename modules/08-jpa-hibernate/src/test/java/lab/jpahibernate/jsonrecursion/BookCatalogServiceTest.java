package lab.jpahibernate.jsonrecursion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookCatalogServiceTest {

    private EntityManager entityManager;
    private BookCatalogService service;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        service = new BookCatalogService(entityManager);
    }

    @Test
    @DisplayName("listBooks returns flat DTO projection without circular reference")
    void listBooks_returnsDtoProjection() {
        @SuppressWarnings("unchecked")
        TypedQuery<BookResponseDto> query = mock(TypedQuery.class);
        BookResponseDto dto =
                new BookResponseDto(1L, "Effective Java", "978-0134685991", 10L, "Joshua Bloch");

        when(entityManager.createQuery(anyString(), eq(BookResponseDto.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dto));

        List<BookResponseDto> result = service.listBooks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Effective Java");
        assertThat(result.get(0).authorName()).isEqualTo("Joshua Bloch");
    }

    @Test
    @DisplayName("getAuthorWithBooks returns nested DTO")
    void getAuthorWithBooks_returnsAuthorDtoWithBooks() {
        Author author = new Author("Joshua Bloch");
        author.addBook(new Book("Effective Java", "978-0134685991"));

        when(entityManager.find(Author.class, 1L)).thenReturn(author);

        AuthorResponseDto result = service.getAuthorWithBooks(1L);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Joshua Bloch");
        assertThat(result.books()).hasSize(1);
        assertThat(result.books().get(0).title()).isEqualTo("Effective Java");
    }
}
