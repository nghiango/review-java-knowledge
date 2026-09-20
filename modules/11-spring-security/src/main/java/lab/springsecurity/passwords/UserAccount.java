package lab.springsecurity.passwords;

import java.util.UUID;

public record UserAccount(UUID id, String username, String passwordHash, String role) {}
