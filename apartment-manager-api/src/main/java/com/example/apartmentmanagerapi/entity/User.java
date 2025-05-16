package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.Collection; // For Spring Security roles

// Spring Security specific imports - will be used later
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.userdetails.UserDetails;
// import java.util.List;
// import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // "user" is often a reserved keyword in SQL
public class User { // Consider implementing UserDetails later for Spring Security integration

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // Will be stored encrypted

    @Column(nullable = false, unique = true)
    private String email;

    private String firstName;

    private String lastName;

    private String role; // e.g., "ROLE_MANAGER", "ROLE_TENANT"

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // If you decide to integrate Spring Security's UserDetails directly:
    // @Override
    // public Collection<? extends GrantedAuthority> getAuthorities() {
    //     // Assuming 'role' is a simple string like "ROLE_MANAGER"
    //     // If you have multiple roles per user, you'd store them differently (e.g., a Set<Role> entity)
    //     return List.of(new SimpleGrantedAuthority(this.role));
    // }

    // @Override
    // public boolean isAccountNonExpired() {
    //     return true;
    // }

    // @Override
    // public boolean isAccountNonLocked() {
    //     return true;
    // }

    // @Override
    // public boolean isCredentialsNonExpired() {
    //     return true;
    // }

    // @Override
    // public boolean isEnabled() {
    //     return true;
    // }
}