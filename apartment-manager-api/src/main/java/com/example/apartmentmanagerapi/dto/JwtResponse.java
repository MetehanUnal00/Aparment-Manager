package com.example.apartmentmanagerapi.dto;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private Collection<? extends GrantedAuthority> roles; // Or List<String> roles

    public JwtResponse(String accessToken, Long id, String username, String email, Collection<? extends GrantedAuthority> roles) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}