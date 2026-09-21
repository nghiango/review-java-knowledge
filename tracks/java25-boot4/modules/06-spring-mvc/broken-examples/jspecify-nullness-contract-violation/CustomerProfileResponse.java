package lab.java25boot4.springmvc.broken.nullness;

public record CustomerProfileResponse(
        String customerId,
        String fullName,
        String phoneNumber
) {}
