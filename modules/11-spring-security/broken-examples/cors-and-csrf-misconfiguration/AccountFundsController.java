package lab.springsecurity.broken.csrf;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountFundsController {

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    public AccountFundsController() {
        balances.put("alice", new BigDecimal("5000.00"));
        balances.put("bob", new BigDecimal("2000.00"));
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(Authentication auth) {
        String username = auth.getName();
        BigDecimal balance = balances.getOrDefault(username, BigDecimal.ZERO);
        return ResponseEntity.ok(Map.of("user", username, "balance", balance));
    }

    // Vulnerable to CSRF: session cookie automatically sent by victim browser from attacker site
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(Authentication auth, @RequestBody TransferRequest request) {
        String sender = auth.getName();
        BigDecimal senderBalance = balances.getOrDefault(sender, BigDecimal.ZERO);

        if (senderBalance.compareTo(request.amount()) < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient funds"));
        }

        balances.put(sender, senderBalance.subtract(request.amount()));
        balances.compute(request.recipient(), (k, v) -> (v == null ? BigDecimal.ZERO : v).add(request.amount()));

        return ResponseEntity.ok(Map.of(
                "status", "TRANSFER_COMPLETED",
                "from", sender,
                "to", request.recipient(),
                "amount", request.amount()));
    }

    public record TransferRequest(String recipient, BigDecimal amount) {}
}
