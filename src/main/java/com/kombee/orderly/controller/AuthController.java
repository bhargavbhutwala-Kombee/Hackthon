package com.kombee.orderly.controller;

import com.kombee.orderly.dto.auth.AuthResponse;
import com.kombee.orderly.dto.auth.LoginRequest;
import com.kombee.orderly.dto.auth.RegisterRequest;
import com.kombee.orderly.service.AuthService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final Tracer tracer;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        Span span = tracer.spanBuilder("controller.auth.register").startSpan();
        try (var scope = span.makeCurrent()) {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } finally {
            span.end();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Span span = tracer.spanBuilder("controller.auth.login").startSpan();
        try (var scope = span.makeCurrent()) {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } finally {
            span.end();
        }
    }
}
