package lab.jpahibernate.dtomapping;

public record UserProfileDto(Long id, String username, String email, boolean admin) {}
