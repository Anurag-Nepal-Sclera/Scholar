package com.scholar.config.security;

import com.scholar.domain.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScholarUserDetailsService implements UserDetailsService {

    private final UserProfileRepository userProfileRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userProfileRepository.findByEmail(email)
                .map(ScholarUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
