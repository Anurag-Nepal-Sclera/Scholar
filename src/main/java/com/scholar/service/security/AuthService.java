package com.scholar.service.security;

import com.scholar.config.security.ScholarUserDetails;
import com.scholar.domain.entity.Tenant;
import com.scholar.domain.entity.UserProfile;
import com.scholar.domain.repository.TenantRepository;
import com.scholar.domain.repository.UserProfileRepository;
import com.scholar.dto.request.AuthenticationRequest;
import com.scholar.dto.request.RegisterRequest;
import com.scholar.dto.response.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserProfileRepository repository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        var user = UserProfile.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserProfile.UserRole.USER)
                .status(UserProfile.UserStatus.ACTIVE)
                .build();
        
        var savedUser = repository.save(user);
        var userDetails = new ScholarUserDetails(savedUser);
        var jwtToken = jwtService.generateToken(userDetails);
        
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        var userDetails = new ScholarUserDetails(user);
        var jwtToken = jwtService.generateToken(userDetails);
        
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
