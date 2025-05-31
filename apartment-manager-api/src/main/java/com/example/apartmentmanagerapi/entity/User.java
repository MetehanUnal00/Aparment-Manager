package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection; // For Spring Security roles
import java.util.List;

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

    /**
     * User role - using enum for type safety
     * Supports ADMIN, MANAGER, and VIEWER roles as per requirements
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.VIEWER; // Default to least privileged role
    
    /**
     * List of building assignments for this user
     * Managers can be assigned to multiple buildings
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserBuildingAssignment> buildingAssignments = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * User roles supported by the system
     */
    public enum UserRole {
        ADMIN("Admin"),         // Full system access
        MANAGER("Manager"),     // Can manage assigned buildings
        VIEWER("Viewer");       // Read-only access
        
        private final String displayName;
        
        UserRole(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * Get the Spring Security role name
         * @return Role name with ROLE_ prefix
         */
        public String getRoleName() {
            return "ROLE_" + this.name();
        }
    }
    
    /**
     * Helper method to check if user has admin role
     * @return true if user is an admin
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    /**
     * Helper method to check if user has manager role
     * @return true if user is a manager
     */
    public boolean isManager() {
        return role == UserRole.MANAGER;
    }
    
    /**
     * Helper method to get active building assignments
     * @return List of active assignments only
     */
    public List<UserBuildingAssignment> getActiveBuildingAssignments() {
        return buildingAssignments.stream()
                .filter(UserBuildingAssignment::isCurrentlyActive)
                .toList();
    }

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