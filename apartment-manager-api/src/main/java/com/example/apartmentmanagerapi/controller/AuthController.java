package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.config.JwtUtil;
import com.example.apartmentmanagerapi.dto.JwtResponse;
import com.example.apartmentmanagerapi.dto.LoginRequest;
import com.example.apartmentmanagerapi.dto.MessageResponse;
import com.example.apartmentmanagerapi.dto.SignupRequest;
import com.example.apartmentmanagerapi.entity.User;
import com.example.apartmentmanagerapi.repository.UserRepository;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@CrossOrigin(origins = "*", maxAge = 3600) // Allow all origins for now
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateToken((UserDetails) authentication.getPrincipal());

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // Assuming your UserDetails implementation (from UserDetailsServiceImpl)
        // correctly populates authorities based on the User entity's role.
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // Fetch user ID and email if needed for the response
        // This requires your UserDetails to be an instance of your User entity or have access to its properties
        // For simplicity, let's assume we can get it from the principal if it's our custom User object
        // or we fetch it again. For now, we'll just use username from UserDetails.
        // If you need more user details in JwtResponse, you might need to fetch the User entity here.
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);


        return ResponseEntity.ok(new JwtResponse(jwt,
            user != null ? user.getId() : null, // Handle if user is null
            userDetails.getUsername(),
            user != null ? user.getEmail() : null, // Handle if user is null
            userDetails.getAuthorities()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());

        // Set a default role for new users
        // For security, new users get VIEWER role by default
        // Admins can later upgrade them to MANAGER role if needed
        user.setRole(User.UserRole.VIEWER); // Default to least privileged role

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}