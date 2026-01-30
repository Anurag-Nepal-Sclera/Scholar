package com.scholar.controller;

import com.scholar.dto.request.AuthenticationRequest;
import com.scholar.dto.request.RegisterRequest;
import com.scholar.dto.response.ApiResponse;
import com.scholar.dto.response.AuthenticationResponse;
import com.scholar.service.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user profile and organization")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", service.register(request)));
    }

    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate user", description = "Login with email and password to receive a JWT token")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", service.authenticate(request)));
    }
}
