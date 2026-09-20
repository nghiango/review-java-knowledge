package lab.springsecurity.jwt;

import java.util.List;

public record JwtClaims(
        String subject, String email, List<String> roles, long exp, String issuer) {}
