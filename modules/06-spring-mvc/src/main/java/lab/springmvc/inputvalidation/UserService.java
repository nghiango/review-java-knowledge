package lab.springmvc.inputvalidation;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public UserResponse registerUser(CreateUserRequest request) {
        String generatedId = UUID.randomUUID().toString();
        return new UserResponse(
                generatedId, request.username(), request.email(), request.initialBalance());
    }
}
