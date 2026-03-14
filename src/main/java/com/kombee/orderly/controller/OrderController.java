package com.kombee.orderly.controller;

import com.kombee.orderly.dto.common.PageResponse;
import com.kombee.orderly.dto.order.OrderRequest;
import com.kombee.orderly.dto.order.OrderResponse;
import com.kombee.orderly.security.UserPrincipal;
import com.kombee.orderly.service.OrderService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final Tracer tracer;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OrderRequest request) {
        Span span = tracer.spanBuilder("controller.orders.create").startSpan();
        try (var scope = span.makeCurrent()) {
            Long userId = principal != null ? principal.getUserId() : null;
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }
            OrderResponse created = orderService.create(userId, request);
            return ResponseEntity.status(201).body(created);
        } finally {
            span.end();
        }
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Span span = tracer.spanBuilder("controller.orders.list").startSpan();
        try (var scope = span.makeCurrent()) {
            Long userId = principal != null ? principal.getUserId() : null;
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }
            PageResponse<OrderResponse> result = orderService.findByUser(userId, page, size, status);
            return ResponseEntity.ok(result);
        } finally {
            span.end();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Span span = tracer.spanBuilder("controller.orders.getById").startSpan();
        try (var scope = span.makeCurrent()) {
            Long userId = principal != null ? principal.getUserId() : null;
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }
            OrderResponse order = orderService.getById(userId, id);
            return ResponseEntity.ok(order);
        } finally {
            span.end();
        }
    }
}
