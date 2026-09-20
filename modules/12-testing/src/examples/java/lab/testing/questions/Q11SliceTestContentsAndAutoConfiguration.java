package lab.testing.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.List;
import lab.testing.accounts.AccountRepository;
import lab.testing.accounts.AccountService;
import lab.testing.fulfilment.InventoryResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

/**
 * Q11: What a test slice contains, and how the auto-configuration decides it.
 *
 * <p>A slice is {@code @OverrideAutoConfiguration(enabled = false)} plus a curated list of
 * auto-configurations, so it is defined as much by what it *leaves out* as by what it loads. The
 * five classes below are the five contexts this module uses: the whole application, the web layer
 * (with the service replaced by a mock bean), JPA plus a DataSource, the Jackson testers, and one
 * HTTP client plus a mock server. {@code AccountRepositoryIT} is the {@code @DataJpaTest} row with
 * {@code replace = NONE}, which is what keeps the container-backed DataSource instead of swapping
 * in an embedded one. The example never starts a context; {@code main} reads the annotations back.
 */
public class Q11SliceTestContentsAndAutoConfiguration {

    /** The whole application: every bean, a web server and a real DataSource. */
    @SpringBootTest
    @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
    static class FullContextTest {

        private final AccountService accountService;

        FullContextTest(AccountService accountService) {
            this.accountService = accountService;
        }

        @Test
        void everyCollaboratorIsAvailable() {
            assertThat(accountService).isInstanceOf(AccountService.class);
        }
    }

    /** The web layer only: controllers, filters and advice; the service is a mock bean. */
    @WebMvcTest
    static class WebLayerTest {

        @MockitoBean private AccountService accountService;

        @Test
        void theServiceInsideTheSliceIsAMock() {
            assertThat(Mockito.mockingDetails(accountService).isMock()).isTrue();
        }
    }

    /** JPA plus a DataSource: no web layer, and the slice is transactional. */
    @DataJpaTest
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
    @Import(AccountService.class)
    @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
    static class JpaSliceTest {

        private final AccountRepository repository;

        JpaSliceTest(AccountRepository repository) {
            this.repository = repository;
        }

        @Test
        void theRepositoryIsTheRealSpringDataProxy() {
            assertThat(repository.findAll()).isEmpty();
        }
    }

    /** Only the Jackson testers: no web layer, no DataSource. */
    @JsonTest
    static class JsonSliceTest {

        @Test
        void theJsonTesterWritesTheWireFormat(@Autowired JacksonTester<InventoryResponse> json)
                throws IOException {
            assertThat(json.write(new InventoryResponse("A-1", 3)).getJson())
                    .contains("\"available\":3");
        }
    }

    /** One HTTP client plus a mock server: the outbound call is stubbed, not the client mocked. */
    @RestClientTest
    static class RestClientSliceTest {

        @Test
        void theSliceProvidesAMockRestServiceServer(@Autowired MockRestServiceServer server) {
            server.expect(requestTo("/inventory/A-1"))
                    .andRespond(
                            withSuccess(
                                    "{\"sku\":\"A-1\",\"available\":3}",
                                    MediaType.APPLICATION_JSON));
        }
    }

    public static void main(String[] args) {
        List<Class<?>> slices =
                List.of(
                        WebLayerTest.class,
                        JpaSliceTest.class,
                        JsonSliceTest.class,
                        RestClientSliceTest.class);

        boolean jpaSliceReplacesTheDataSource =
                DataJpaTest.class.isAnnotationPresent(AutoConfigureTestDatabase.class); // true
        boolean webSliceAutoConfiguresMockMvc =
                WebMvcTest.class.isAnnotationPresent(AutoConfigureMockMvc.class); // true
        boolean jsonSliceLoadsTheJsonTesters =
                JsonTest.class.isAnnotationPresent(AutoConfigureJsonTesters.class); // true
        boolean restClientSliceLoadsAMockServer =
                RestClientTest.class.isAnnotationPresent(
                        AutoConfigureMockRestServiceServer.class); // true
        boolean fullContextIsASlice =
                SpringBootTest.class.isAnnotationPresent(AutoConfigureTestDatabase.class); // false

        AutoConfigureTestDatabase.Replace defaultReplace =
                DataJpaTest.class
                        .getAnnotation(AutoConfigureTestDatabase.class)
                        .replace(); // NON_TEST
        AutoConfigureTestDatabase.Replace sliceReplace =
                JpaSliceTest.class.getAnnotation(AutoConfigureTestDatabase.class).replace(); // NONE
        long slicesImportingExtraBeans =
                slices.stream()
                        .filter(slice -> slice.isAnnotationPresent(Import.class))
                        .count(); // 1
        long slicesWithMockBeans =
                slices.stream()
                        .filter(slice -> slice.isAnnotationPresent(WebMvcTest.class))
                        .count(); // 1

        System.out.println(
                "JPA replaces DS: " + jpaSliceReplacesTheDataSource); // JPA replaces DS: true
        System.out.println(
                "Web mocks MVC: " + webSliceAutoConfiguresMockMvc); // Web mocks MVC: true
        System.out.println("Json testers: " + jsonSliceLoadsTheJsonTesters); // Json testers: true
        System.out.println("Mock server: " + restClientSliceLoadsAMockServer); // Mock server: true
        System.out.println(
                "Full context is a slice: "
                        + fullContextIsASlice); // Full context is a slice: false
        System.out.println("Slices: " + slices.size()); // Slices: 4
        System.out.println("Default replace: " + defaultReplace); // Default replace: NON_TEST
        System.out.println("Slice replace: " + sliceReplace); // Slice replace: NONE
        System.out.println(
                "Slices with @Import: " + slicesImportingExtraBeans); // Slices with @Import: 1
        System.out.println("Web slices: " + slicesWithMockBeans); // Web slices: 1
    }
}
