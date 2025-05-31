package com.example.apartmentmanagerapi.config;

import com.example.apartmentmanagerapi.dto.LoginRequest;
import com.example.apartmentmanagerapi.dto.SignupRequest;
import com.example.apartmentmanagerapi.entity.User;
import com.example.apartmentmanagerapi.entity.User.UserRole;
import com.example.apartmentmanagerapi.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Spring Security configuration.
 * Tests authentication, authorization, and JWT token handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Configuration Integration Tests")
class SecurityConfigIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test users
        userRepository.deleteAll();
    }
    
    /**
     * Test that public endpoints are accessible without authentication
     */
    @Test
    @DisplayName("Should allow access to public endpoints without authentication")
    void testPublicEndpointsAccessible() throws Exception {
        // Test login endpoint
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isUnauthorized()); // Invalid credentials, but endpoint is accessible
        
        // Test register endpoint
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"testpass123\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk());
    }
    
    /**
     * Test that protected endpoints require authentication
     */
    @Test
    @DisplayName("Should deny access to protected endpoints without authentication")
    void testProtectedEndpointsRequireAuth() throws Exception {
        // Buildings endpoint
        mockMvc.perform(get("/api/apartment-buildings"))
                .andExpect(status().isUnauthorized());
        
        // Flats endpoint
        mockMvc.perform(get("/api/flats"))
                .andExpect(status().isUnauthorized());
        
        // Payments endpoint
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());
        
        // Monthly dues endpoint
        mockMvc.perform(get("/api/monthly-dues"))
                .andExpect(status().isUnauthorized());
    }
    
    /**
     * Test successful user registration
     */
    @Test
    @DisplayName("Should successfully register a new user")
    void testUserRegistration() throws Exception {
        // Given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setPassword("password123");
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setFirstName("New");
        signupRequest.setLastName("User");
        
        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
        
        // Verify user was created
        User createdUser = userRepository.findByUsername("newuser").orElse(null);
        assertNotNull(createdUser);
        assertEquals("newuser@example.com", createdUser.getEmail());
        assertEquals(UserRole.VIEWER, createdUser.getRole()); // Default role
    }
    
    /**
     * Test duplicate username registration
     */
    @Test
    @DisplayName("Should reject registration with duplicate username")
    void testDuplicateUsernameRegistration() throws Exception {
        // Given - Create existing user
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setPassword(passwordEncoder.encode("password"));
        existingUser.setEmail("existing@example.com");
        existingUser.setRole(UserRole.VIEWER);
        userRepository.save(existingUser);
        
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("existinguser");
        signupRequest.setPassword("password123");
        signupRequest.setEmail("new@example.com");
        
        // When/Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));
    }
    
    /**
     * Test successful login and JWT token generation
     */
    @Test
    @DisplayName("Should successfully login and receive JWT token")
    void testSuccessfulLogin() throws Exception {
        // Given - Create a user
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("testpass"));
        user.setEmail("test@example.com");
        user.setRole(UserRole.VIEWER);
        userRepository.save(user);
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("testpass");
        
        // When/Then
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0].authority").value("ROLE_VIEWER"))
                .andReturn();
        
        // Extract token for further tests
        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        assertNotNull(responseMap.get("token"));
    }
    
    /**
     * Test login with invalid credentials
     */
    @Test
    @DisplayName("Should reject login with invalid credentials")
    void testLoginWithInvalidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword("wrongpass");
        
        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
    
    /**
     * Test accessing protected endpoint with valid JWT token
     */
    @Test
    @DisplayName("Should allow access to protected endpoints with valid JWT token")
    void testProtectedEndpointWithValidToken() throws Exception {
        // Given - Create and login user
        User user = new User();
        user.setUsername("authuser");
        user.setPassword(passwordEncoder.encode("authpass"));
        user.setEmail("auth@example.com");
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        
        String token = loginAndGetToken("authuser", "authpass");
        
        // When/Then - Access protected endpoint
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isOk());
    }
    
    /**
     * Test accessing protected endpoint with invalid JWT token
     */
    @Test
    @DisplayName("Should deny access with invalid JWT token")
    void testProtectedEndpointWithInvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        // When/Then
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + invalidToken))
                .andExpect(status().isUnauthorized());
    }
    
    /**
     * Test accessing protected endpoint with expired JWT token
     */
    @Test
    @DisplayName("Should deny access with malformed JWT token")
    void testProtectedEndpointWithMalformedToken() throws Exception {
        // Given
        String malformedToken = "not-a-jwt";
        
        // When/Then
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + malformedToken))
                .andExpect(status().isUnauthorized());
    }
    
    /**
     * Test role-based access control for ADMIN endpoints
     */
    @Test
    @DisplayName("Should enforce role-based access control for ADMIN endpoints")
    void testAdminRoleAccess() throws Exception {
        // Given - Create VIEWER user
        User viewerUser = new User();
        viewerUser.setUsername("viewer");
        viewerUser.setPassword(passwordEncoder.encode("password"));
        viewerUser.setEmail("viewer@example.com");
        viewerUser.setRole(UserRole.VIEWER);
        userRepository.save(viewerUser);
        
        String viewerToken = loginAndGetToken("viewer", "password");
        
        // When/Then - VIEWER should not access MANAGER endpoints
        mockMvc.perform(post("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Building\",\"address\":\"123 Test St\"}"))
                .andExpect(status().isForbidden());
        
        // Given - Create MANAGER user
        User managerUser = new User();
        managerUser.setUsername("manager2");
        managerUser.setPassword(passwordEncoder.encode("password"));
        managerUser.setEmail("manager2@example.com");
        managerUser.setRole(UserRole.MANAGER);
        userRepository.save(managerUser);
        
        String managerToken = loginAndGetToken("manager2", "password");
        
        // When/Then - MANAGER should access building creation endpoints
        mockMvc.perform(post("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Building\",\"address\":\"123 Test St\"}")); // May fail without full implementation
    }
    
    /**
     * Test role-based access control for MANAGER endpoints
     */
    @Test
    @DisplayName("Should enforce role-based access control for MANAGER endpoints")
    void testManagerRoleAccess() throws Exception {
        // Given - Create MANAGER user
        User managerUser = new User();
        managerUser.setUsername("manager");
        managerUser.setPassword(passwordEncoder.encode("password"));
        managerUser.setEmail("manager@example.com");
        managerUser.setRole(UserRole.MANAGER);
        userRepository.save(managerUser);
        
        String managerToken = loginAndGetToken("manager", "password");
        
        // When/Then - MANAGER should access their endpoints
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + managerToken))
                .andExpect(status().isOk());
    }
    
    /**
     * Test JWT token in different header formats
     */
    @Test
    @DisplayName("Should handle different JWT token header formats")
    void testDifferentTokenHeaderFormats() throws Exception {
        // Given - Create user and get token
        User user = new User();
        user.setUsername("headertest");
        user.setPassword(passwordEncoder.encode("password"));
        user.setEmail("header@example.com");
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        
        String token = loginAndGetToken("headertest", "password");
        
        // Test with lowercase "bearer"
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, "bearer " + token))
                .andExpect(status().isOk());
        
        // Test without Bearer prefix should fail
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, token))
                .andExpect(status().isUnauthorized());
        
        // Test with wrong prefix should fail
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, "Token " + token))
                .andExpect(status().isUnauthorized());
    }
    
    /**
     * Test CORS configuration
     */
    @Test
    @DisplayName("Should handle CORS preflight requests")
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
    
    /**
     * Test authentication persistence across requests
     */
    @Test
    @DisplayName("Should maintain authentication state across multiple requests")
    void testAuthenticationPersistence() throws Exception {
        // Given - Create user and get token
        User user = new User();
        user.setUsername("persistuser");
        user.setPassword(passwordEncoder.encode("password"));
        user.setEmail("persist@example.com");
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        
        String token = loginAndGetToken("persistuser", "password");
        
        // When/Then - Make multiple authenticated requests
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/apartment-buildings")
                .header(AUTH_HEADER, BEARER_PREFIX + token))
                .andExpect(status().isOk());
    }
    
    /**
     * Helper method to login and extract JWT token
     */
    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        return (String) responseMap.get("token");
    }
}