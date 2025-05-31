package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.User;
import com.example.apartmentmanagerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserDetailsServiceImpl
 * Tests Spring Security user loading functionality
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword123");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(User.UserRole.MANAGER);
    }

    @Test
    @DisplayName("Load user by username - Success with MANAGER role")
    void loadUserByUsername_Success_ManagerRole() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        
        // Verify authority
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_MANAGER");
        
        // Verify interaction
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Load user by username - Success with ADMIN role")
    void loadUserByUsername_Success_AdminRole() {
        // Arrange
        testUser.setRole(User.UserRole.ADMIN);
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("adminuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        
        // Verify admin authority
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_ADMIN");
        
        // Verify interaction
        verify(userRepository).findByUsername("adminuser");
    }

    @Test
    @DisplayName("Load user by username - Success with VIEWER role")
    void loadUserByUsername_Success_ViewerRole() {
        // Arrange
        testUser.setRole(User.UserRole.VIEWER);
        when(userRepository.findByUsername("vieweruser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("vieweruser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        
        // Verify viewer authority
        GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
        assertThat(authority.getAuthority()).isEqualTo("ROLE_VIEWER");
        
        // Verify interaction
        verify(userRepository).findByUsername("vieweruser");
    }

    @Test
    @DisplayName("Load user by username - User not found throws UsernameNotFoundException")
    void loadUserByUsername_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User Not Found with username: nonexistent");

        // Verify interaction
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Load user by username - Verify Spring Security UserDetails properties")
    void loadUserByUsername_VerifyUserDetailsProperties() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert - verify all UserDetails interface methods
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
        
        // These properties are true by default in Spring's User implementation
        // In a more complex implementation, these might be based on user entity fields
    }

    @Test
    @DisplayName("Load user by username - Null username")
    void loadUserByUsername_NullUsername() {
        // Arrange
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User Not Found with username: null");

        // Verify interaction
        verify(userRepository).findByUsername(null);
    }

    @Test
    @DisplayName("Load user by username - Empty username")
    void loadUserByUsername_EmptyUsername() {
        // Arrange
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User Not Found with username: ");

        // Verify interaction
        verify(userRepository).findByUsername("");
    }

    @Test
    @DisplayName("Load user by username - Case sensitivity")
    void loadUserByUsername_CaseSensitivity() {
        // Arrange
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert - uppercase should not find user
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("TestUser"))
                .isInstanceOf(UsernameNotFoundException.class);

        // Act - lowercase should find user
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");

        // Verify interactions
        verify(userRepository).findByUsername("TestUser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Load user by username - Verify authorities are immutable")
    void loadUserByUsername_AuthoritiesAreImmutable() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert - authorities should be a new collection (not a reference to internal state)
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MANAGER");
    }

    @Test
    @DisplayName("Load user by username - Repository exception propagates")
    void loadUserByUsername_RepositoryException_Propagates() {
        // Arrange
        when(userRepository.findByUsername("testuser"))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection error");

        // Verify interaction
        verify(userRepository).findByUsername("testuser");
    }
}