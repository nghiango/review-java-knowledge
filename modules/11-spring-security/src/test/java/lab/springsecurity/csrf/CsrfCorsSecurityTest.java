package lab.springsecurity.csrf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CsrfCorsSecurityTest {

    private SafeFundsTransferController controller;

    @BeforeEach
    void setUp() {
        controller = new SafeFundsTransferController();
    }

    @Test
    @DisplayName("Funds transfer executes successfully when balance is sufficient")
    void transfer_sufficientFunds_updatesBalances() {
        var auth =
                new UsernamePasswordAuthenticationToken(
                        "alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var request =
                new SafeFundsTransferController.TransferRequest("bob", new BigDecimal("500.00"));

        ResponseEntity<Map<String, Object>> response = controller.transfer(auth, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "TRANSFER_COMPLETED");
        assertThat(response.getBody()).containsEntry("amount", new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Funds transfer returns 400 Bad Request when balance is insufficient")
    void transfer_insufficientFunds_returnsBadRequest() {
        var auth =
                new UsernamePasswordAuthenticationToken(
                        "alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var request =
                new SafeFundsTransferController.TransferRequest("bob", new BigDecimal("10000.00"));

        ResponseEntity<Map<String, Object>> response = controller.transfer(auth, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("error");
    }
}
