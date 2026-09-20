# Solution: Bidirectional JSON Serialization Recursion

## Annotated Code

```java
package lab.jpahibernate.broken.jsonrecursion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors")
public class Author {

    @Id private String id;
    private String name;

    // Reliability issue: bidirectional relationship without @JsonIgnore or DTO projection causes infinite JSON recursion (StackOverflowError)
    @OneToMany(mappedBy = "author")
    private List<Book> books = new ArrayList<>();

    public Author() {}

    public Author(String id, String name, List<Book> books) {
        this.id = id;
        this.name = name;
        this.books = books;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Book> getBooks() {
        return books;
    }
}
```

## Issues Identified

### Reliability issue: bidirectional entity serialization causes infinite recursion and StackOverflowError
- **Location:** `Author.java#books`, `BookCatalogController.java#getAuthorAsJson`
- **Explanation:** When Jackson serializes `Author`, it attempts to serialize `books`. Each `Book` contains a reference back to `Author`, prompting Jackson to serialize the `Author` again, continuing infinitely until a `StackOverflowError` crashes the thread. REST endpoints should project entities into clean, unidirectional record DTOs or annotate reverse relationships with `@JsonIgnore` / `@JsonBackReference`.

## Correct implementation

See `lab.jpahibernate.jsonrecursion.AuthorDto`, `lab.jpahibernate.jsonrecursion.BookDto`, and `lab.jpahibernate.jsonrecursion.BookCatalogService`.
