package com.example.apartmentmanagerapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    private String firstName;

    private String lastName;

    // For simplicity, we'll assign a default role or handle role assignment separately.
    // You could add a 'role' field here if clients can specify it,
    // but ensure proper validation/authorization for that.
    // private String role;
}