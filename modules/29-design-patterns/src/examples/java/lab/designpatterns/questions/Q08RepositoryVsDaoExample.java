package lab.designpatterns.questions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Q08: Repository vs Data Access Object (DAO) Pattern. Demonstrates Repository (in-memory
 * collection illusion for domain aggregates) versus DAO (table/row-oriented database mapping).
 */
public class Q08RepositoryVsDaoExample {

    public record User(String id, String username) {}

    // Repository: Collection-like interface working with Domain Entities
    public interface UserRepository {
        void add(User user);

        Optional<User> findById(String id);
    }

    public static class InMemoryUserRepository implements UserRepository {
        private final Map<String, User> store = new HashMap<>();

        @Override
        public void add(User user) {
            store.put(user.id(), user);
        }

        @Override
        public Optional<User> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    public static void main(String[] args) {
        UserRepository repo = new InMemoryUserRepository();
        repo.add(new User("u-42", "antigravity"));

        Optional<User> user = repo.findById("u-42");
        boolean exists = user.isPresent() && user.get().username().equals("antigravity"); // true

        System.out.println("Q08 repoFound: " + exists);
    }
}
