package lab.springsecurity.broken.passwords;

import java.util.UUID;

public record UserAccount(UUID id, String username, String storedPassword, String role) {}
