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

    // Bidirectional relationship referencing Books without @JsonIgnore or DTO mapping
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
