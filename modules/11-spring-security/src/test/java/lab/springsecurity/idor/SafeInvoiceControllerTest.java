package lab.springsecurity.idor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class SafeInvoiceControllerTest {

    private InvoiceService invoiceService;
    private SafeInvoiceController controller;
    private UUID aliceInvoiceId;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService();
        controller = new SafeInvoiceController(invoiceService);
        aliceInvoiceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("Owner can access their own invoice")
    void getInvoice_byOwner_succeedsWith200() {
        var auth =
                new UsernamePasswordAuthenticationToken(
                        "alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        ResponseEntity<Invoice> response = controller.getInvoice(aliceInvoiceId, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ownerUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Non-owner user is rejected with AccessDeniedException (HTTP 403)")
    void getInvoice_byOtherUser_throwsAccessDeniedException() {
        var auth =
                new UsernamePasswordAuthenticationToken(
                        "bob", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThatThrownBy(() -> controller.getInvoice(aliceInvoiceId, auth))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Admin user can access any customer's invoice")
    void getInvoice_byAdmin_succeedsWith200() {
        var auth =
                new UsernamePasswordAuthenticationToken(
                        "admin_user", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        ResponseEntity<Invoice> response = controller.getInvoice(aliceInvoiceId, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ownerUsername()).isEqualTo("alice");
    }
}
