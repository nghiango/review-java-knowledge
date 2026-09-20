package lab.springmvc.broken.corssecurity;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*", allowCredentials = "true")
public class AccountController {

    @GetMapping("/profile")
    public String getProfile() {
        return "Sensitive account data";
    }
}
