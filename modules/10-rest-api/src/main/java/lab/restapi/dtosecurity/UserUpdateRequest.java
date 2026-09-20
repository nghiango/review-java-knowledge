package lab.restapi.dtosecurity;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(@NotBlank String fullName) {}
