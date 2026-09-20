package lab.testing;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Minimal application used to bootstrap the Spring test slices of this module.
 *
 * <p>{@code @DataJpaTest} needs a {@code @SpringBootConfiguration} it can discover by walking up
 * from the test's package. Placing it at {@code lab.testing} means the auto-configuration's
 * repository and entity scans start there and cover {@code lab.testing.accounts}, where the
 * entities and repositories of this module live.
 *
 * <p>It is not a runnable application: it exists only so the integration tests get a real
 * auto-configured context without a {@code main} method or a web server.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TestingApplication {}
