package com.example.apartmentmanagerapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for user registration request.
 * Contains user information required for creating a new account.
 */
@Data
public class SignupRequest {
    /**
     * Username for the new account
     * Must be unique, between 3-20 characters
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    /**
     * Email address for the account
     * Must be a valid email format
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    /**
     * Password for the account
     * Must be between 6-40 characters
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;

    /**
     * User's first name (optional)
     */
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s-]*$", message = "First name can only contain letters, spaces, and hyphens")
    private String firstName;

    /**
     * User's last name (optional)
     */
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s-]*$", message = "Last name can only contain letters, spaces, and hyphens")
    private String lastName;

    // For simplicity, we'll assign a default role or handle role assignment separately.
    // You could add a 'role' field here if clients can specify it,
    // but ensure proper validation/authorization for that.
    // private String role;
}