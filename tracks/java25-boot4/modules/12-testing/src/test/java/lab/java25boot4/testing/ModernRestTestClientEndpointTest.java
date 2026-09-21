package lab.java25boot4.testing;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ModernRestTestClientEndpointTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ModernOrderApiController()).build();
    }

    @Test
    @DisplayName("should create order and return 201 Created with JSON response")
    void shouldCreateOrderSuccessfully() throws Exception {
        String requestJson =
                """
                {
                    "customerId": "cust-99",
                    "sku": "SKU-ABC",
                    "quantity": 3,
                    "promoCode": "DISCOUNT10"
                }
                """;

        mockMvc.perform(
                        post("/api/v2/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                        "Content-Type",
                                        containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.customerId", is("cust-99")))
                .andExpect(jsonPath("$.sku", is("SKU-ABC")))
                .andExpect(jsonPath("$.quantity", is(3)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    @DisplayName("should return RFC 9457 ProblemDetail 404 Not Found when order missing")
    void shouldReturnProblemDetailForNotFound() throws Exception {
        mockMvc.perform(
                        get("/api/v2/orders/ord-unknown")
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON,
                                        MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Order Not Found")))
                .andExpect(jsonPath("$.detail", containsString("ord-unknown")))
                .andExpect(
                        jsonPath("$.type", is("https://api.example.com/errors/order-not-found")));
    }

    @Test
    @DisplayName(
            "should return RFC 9457 ProblemDetail 422 Unprocessable Entity with extension property")
    void shouldReturnProblemDetailForInvalidPromo() throws Exception {
        String requestJson =
                """
                {
                    "customerId": "cust-99",
                    "sku": "SKU-ABC",
                    "quantity": 1,
                    "promoCode": "EXPIRED"
                }
                """;

        mockMvc.perform(
                        post("/api/v2/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.title", is("Invalid Promo Code")))
                .andExpect(jsonPath("$.promoCode", is("EXPIRED")))
                .andExpect(jsonPath("$.type", is("https://api.example.com/errors/invalid-promo")));
    }
}
