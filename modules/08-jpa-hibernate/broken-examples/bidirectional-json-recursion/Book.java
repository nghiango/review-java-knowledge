package lab.jpahibernate.broken.jsonrecursion;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id private String id;
    private String title;

    // Bidirectional relationship referencing Author
    @ManyToOne private Author author;

    public Book() {}

    public Book(String id, String title, Author author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Author getAuthor() {
        return author;
    }
}
