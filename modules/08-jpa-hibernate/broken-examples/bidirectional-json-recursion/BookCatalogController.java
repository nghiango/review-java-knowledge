package lab.jpahibernate.broken.jsonrecursion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookCatalogController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getAuthorAsJson(Author author) throws Exception {
        // Serializing bidirectional entity directly causes infinite recursion and StackOverflowError in Jackson
        return objectMapper.writeValueAsString(author);
    }

    public String getBookAsJson(Book book) throws Exception {
        // Serializing book directly serializes author which serializes books...
        return objectMapper.writeValueAsString(book);
    }
}
