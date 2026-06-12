package cl.nextech.dashboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDto {

    public record LoginRequest(
        @Email    @NotBlank String email,
        @NotBlank           String password
    ) {}

    public record LoginResponse(
        String token,
        String name,
        String email,
        String role
    ) {}

    public record RegisterRequest(
        @Email    @NotBlank String email,
        @NotBlank           String password,
        @NotBlank           String name
    ) {}
}
