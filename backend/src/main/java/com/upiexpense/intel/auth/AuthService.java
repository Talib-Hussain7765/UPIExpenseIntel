package com.upiexpense.intel.auth;

import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.common.error.UnauthorizedException;
import com.upiexpense.intel.common.security.CurrentUser;
import com.upiexpense.intel.common.security.JwtService;
import com.upiexpense.intel.category.DefaultCategorySeeder;
import com.upiexpense.intel.user.User;
import com.upiexpense.intel.user.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DefaultCategorySeeder defaultCategorySeeder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            DefaultCategorySeeder defaultCategorySeeder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.defaultCategorySeeder = defaultCategorySeeder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        log.debug("Registering user with normalized email={}", email);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Registration rejected because email is already registered: email={}", email);
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());

        User savedUser = userRepository.save(user);
        defaultCategorySeeder.seedForUser(savedUser);
        log.info("User registered successfully: userId={}, email={}", savedUser.getId(), savedUser.getEmail());
        return authResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        log.debug("Authenticating user with normalized email={}", email);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Login rejected because user was not found: email={}", email);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login rejected because password did not match: userId={}, email={}", user.getId(), email);
            throw new UnauthorizedException("Invalid email or password");
        }

        log.info("User logged in successfully: userId={}, email={}", user.getId(), user.getEmail());
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(CurrentUser currentUser) {
        log.debug("Loading current user profile: userId={}", currentUser.id());
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> {
                    log.warn("Authenticated user no longer exists: userId={}", currentUser.id());
                    return new UnauthorizedException("Authenticated user no longer exists");
                });

        log.info("Current user profile loaded: userId={}, email={}", user.getId(), user.getEmail());
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getTimezone(),
                user.getCurrency(),
                user.isMonthlyReportEnabled(),
                user.getMonthlyReportDay()
        );
    }

    private AuthResponse authResponse(User user) {
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getEmail(), user.getPasswordHash(), user.getFullName());
        String token = jwtService.generateToken(currentUser);

        return new AuthResponse(
                "Bearer",
                token,
                jwtService.expiresInSeconds(),
                user.getId(),
                user.getEmail(),
                user.getFullName()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
