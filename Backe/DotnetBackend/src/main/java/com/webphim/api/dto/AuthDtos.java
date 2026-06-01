package com.webphim.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String fullName,
            String phoneNumber) {}

    public record AuthResponse(
            String accessToken,
            String tokenType,
            long expiresIn) {
        public AuthResponse(String accessToken) {
            this(accessToken, "Bearer", 604800);
        }
    }

    public record UserInfoResponse(
            @JsonProperty("id") Integer userId,
            String username,
            String email,
            String fullName,
            String phoneNumber,
            String role,
            int points) {}

    public record UpdateProfileRequest(
            @NotBlank String fullName,
            String phoneNumber) {}
}
