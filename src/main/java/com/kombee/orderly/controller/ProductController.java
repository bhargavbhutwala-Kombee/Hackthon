package com.kombee.orderly.controller;

import com.kombee.orderly.dto.common.PageResponse;
import com.kombee.orderly.dto.product.ProductRequest;
import com.kombee.orderly.dto.product.ProductResponse;
import com.kombee.orderly.service.ProductService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final Tracer tracer;

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String sku) {
        Span span = tracer.spanBuilder("controller.products.list").startSpan();
        try (var scope = span.makeCurrent()) {
            PageResponse<ProductResponse> result = productService.findAll(page, size, name, sku);
            return ResponseEntity.ok(result);
        } finally {
            span.end();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        Span span = tracer.spanBuilder("controller.products.getById").startSpan();
        try (var scope = span.makeCurrent()) {
            ProductResponse product = productService.getById(id);
            return ResponseEntity.ok(product);
        } finally {
            span.end();
        }
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Span span = tracer.spanBuilder("controller.products.create").startSpan();
        try (var scope = span.makeCurrent()) {
            ProductResponse created = productService.create(request);
            return ResponseEntity.status(201).body(created);
        } finally {
            span.end();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Span span = tracer.spanBuilder("controller.products.update").startSpan();
        try (var scope = span.makeCurrent()) {
            ProductResponse updated = productService.update(id, request);
            return ResponseEntity.ok(updated);
        } finally {
            span.end();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Span span = tracer.spanBuilder("controller.products.delete").startSpan();
        try (var scope = span.makeCurrent()) {
            productService.delete(id);
            return ResponseEntity.noContent().build();
        } finally {
            span.end();
        }
    }
}
