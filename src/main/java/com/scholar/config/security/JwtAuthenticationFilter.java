package com.scholar.config.security;

import com.scholar.service.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Senior Java Engineer Fix: 
        // 1. Extract the part after "Bearer "
        // 2. Remove any accidental double "Bearer " prefixes (common Swagger mistake)
        // 3. Remove ALL whitespace characters.
        String token = authHeader.substring(7).trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        // Remove quotes if present (some clients add them accidentally)
        if (token.startsWith("\"") && token.endsWith("\"")) {
            token = token.substring(1, token.length() - 1);
        }
        final String jwt = token.replaceAll("\\s", "");
        
        if (jwt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log specifically if it's a parsing error vs an expired error
            logger.warn("JWT validation failed for token of length " + jwt.length() + ": " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
