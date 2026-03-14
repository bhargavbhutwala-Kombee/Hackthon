package com.kombee.orderly.service;

import com.kombee.orderly.dto.auth.AuthResponse;
import com.kombee.orderly.dto.auth.LoginRequest;
import com.kombee.orderly.dto.auth.RegisterRequest;
import com.kombee.orderly.entity.User;
import com.kombee.orderly.exception.ValidationException;
import com.kombee.orderly.repository.UserRepository;
import com.kombee.orderly.security.JwtUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final Tracer tracer;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Span span = tracer.spanBuilder("auth.register").startSpan();
        try (var scope = span.makeCurrent()) {
            span.setAttribute("username", request.getUsername());
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("Registration failed: username already exists - {}", request.getUsername());
                throw new ValidationException("Username already exists");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Registration failed: email already exists - {}", request.getEmail());
                throw new ValidationException("Email already exists");
            }
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                    .role(User.Role.USER)
                    .build();
            user = userRepository.save(user);
            log.info("User registered successfully: userId={}, username={}", user.getId(), user.getUsername());
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
            return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        } finally {
            span.end();
        }
    }

    public AuthResponse login(LoginRequest request) {
        Span span = tracer.spanBuilder("auth.login").startSpan();
        try (var scope = span.makeCurrent()) {
            span.setAttribute("usernameOrEmail", request.getUsernameOrEmail());
            User user = userRepository.findByUsername(request.getUsernameOrEmail())
                    .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                    .orElse(null);
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                log.warn("Login failed: invalid credentials for {}", request.getUsernameOrEmail());
                throw new ValidationException("Invalid username/email or password");
            }
            log.info("User logged in: userId={}, username={}", user.getId(), user.getUsername());
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
            return AuthResponse.of(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        } finally {
            span.end();
        }
    }
}
