package com.example.apartmentmanagerapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for user login request.
 * Contains username and password for authentication.
 */
@Data
public class LoginRequest {
    /**
     * Username for authentication
     * Must be between 3 and 20 characters
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    /**
     * Password for authentication
     * Must be between 6 and 40 characters
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;
}