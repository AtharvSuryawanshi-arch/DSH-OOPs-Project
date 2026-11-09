package com.example.itsm.security;

import com.example.itsm.user.User;
import com.example.itsm.user.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required.");
        }
        return principal;
    }

    public User requireUser() {
        UserPrincipal principal = requirePrincipal();
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("The authenticated account no longer exists."));
    }

    public Long id() {
        return requirePrincipal().id();
    }
}
