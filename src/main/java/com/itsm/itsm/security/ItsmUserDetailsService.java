package com.itsm.itsm.security;

import com.itsm.itsm.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Spring Security calls this during login to find the user by email.
// It then checks the typed password against the stored hash itself.
@Service
public class ItsmUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public ItsmUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.itsm.itsm.model.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isActive()) // Spring Security refuses the login of a deactivated user
                .build();
    }
}
