package com.upiexpense.intel.auth;

import com.upiexpense.intel.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email={}", request.email());
        return authService.register(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for email={}", request.email());
        return authService.login(request);
    }

    @GetMapping("/me")
    CurrentUserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        log.info("Received current-user request for userId={}", currentUser.id());
        return authService.currentUser(currentUser);
    }
}
