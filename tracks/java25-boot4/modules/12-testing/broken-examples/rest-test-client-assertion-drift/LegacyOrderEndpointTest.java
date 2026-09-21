package lab.java25boot4.testing.broken.assertiondrift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class LegacyOrderEndpointTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerOrderApiController()).build();
    }

    @Test
    public void testInvalidOrderReturnsError() throws Exception {
        mockMvc.perform(get("/api/customer-orders/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testValidOrderReturnsJsonString() throws Exception {
        String responseBody = mockMvc.perform(get("/api/customer-orders/ORD-123"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).isEqualTo("{\"id\":\"ORD-123\",\"amountCents\":4500,\"status\":\"COMPLETED\"}");
    }
}
