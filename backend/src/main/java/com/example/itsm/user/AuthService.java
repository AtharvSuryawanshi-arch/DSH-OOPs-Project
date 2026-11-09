package com.example.itsm.user;

import com.example.itsm.common.BusinessRuleException;
import com.example.itsm.security.CurrentUserService;
import com.example.itsm.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(
            UserRepository userRepository,
            UserFactory userFactory,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = User.normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessRuleException("INVALID_CREDENTIALS", "Invalid email or password.", HttpStatus.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new DisabledException("This account has been deactivated.");
        }
        return AuthResponse.bearer(jwtService.generateToken(user), user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("EMAIL_IN_USE", "An account already exists for this email.");
        }

        String department = request.department() == null || request.department().isBlank()
                ? "General"
                : request.department().trim();

        User user = userFactory.create(
                Role.EMPLOYEE,
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phone(),
                department,
                null,
                null,
                null,
                null
        );
        userRepository.save(user);
        return AuthResponse.bearer(jwtService.generateToken(user), user);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(currentUserService.requireUser());
    }
}
