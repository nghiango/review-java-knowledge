package lab.springsecurity.methodsecurity;

import java.util.UUID;

public record Document(UUID id, String owner, String title, String content) {}
