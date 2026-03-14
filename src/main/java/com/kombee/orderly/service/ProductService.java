package com.kombee.orderly.service;

import com.kombee.orderly.dto.common.PageResponse;
import com.kombee.orderly.dto.product.ProductRequest;
import com.kombee.orderly.dto.product.ProductResponse;
import com.kombee.orderly.entity.Product;
import com.kombee.orderly.exception.ResourceNotFoundException;
import com.kombee.orderly.exception.ValidationException;
import com.kombee.orderly.metrics.ObservabilityMetrics;
import com.kombee.orderly.repository.ProductRepository;
import com.kombee.orderly.util.AnomalyInjector;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final Tracer tracer;
    private final AnomalyInjector anomalyInjector;
    private final ObservabilityMetrics metrics;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(int page, int size, String name, String sku) {
        Span span = tracer.spanBuilder("product.findAll").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/products");
            span.setAttribute("page", page);
            span.setAttribute("size", size);
            Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
            Page<Product> p = productRepository.findAllFiltered(
                    (name != null && !name.isBlank()) ? name.trim() : null,
                    (sku != null && !sku.isBlank()) ? sku.trim() : null,
                    pageable);
            return toPageResponse(p);
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Span span = tracer.spanBuilder("product.getById").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/products");
            span.setAttribute("productId", id);
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
            return toResponse(product);
        } finally {
            span.end();
        }
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Span span = tracer.spanBuilder("product.create").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/products");
            if (productRepository.existsBySku(request.getSku())) {
                log.warn("Product creation failed: SKU already exists - {}", request.getSku());
                throw new ValidationException("Product with SKU already exists: " + request.getSku());
            }
            Product product = Product.builder()
                    .sku(request.getSku().trim())
                    .name(request.getName().trim())
                    .description(request.getDescription() != null ? request.getDescription().trim() : null)
                    .price(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO)
                    .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                    .build();
            product = productRepository.save(product);
            metrics.productCreated();
            log.info("Product created: id={}, sku={}", product.getId(), product.getSku());
            return toResponse(product);
        } finally {
            span.end();
        }
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Span span = tracer.spanBuilder("product.update").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/products");
            span.setAttribute("productId", id);
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
            if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
                throw new ValidationException("Product with SKU already exists: " + request.getSku());
            }
            product.setSku(request.getSku().trim());
            product.setName(request.getName().trim());
            product.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
            product.setPrice(request.getPrice() != null ? request.getPrice() : product.getPrice());
            if (request.getStockQuantity() != null) {
                product.setStockQuantity(request.getStockQuantity());
            }
            product = productRepository.save(product);
            log.info("Product updated: id={}", product.getId());
            return toResponse(product);
        } finally {
            span.end();
        }
    }

    @Transactional
    public void delete(Long id) {
        Span span = tracer.spanBuilder("product.delete").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/products");
            span.setAttribute("productId", id);
            if (!productRepository.existsById(id)) {
                throw new ResourceNotFoundException("Product not found: " + id);
            }
            productRepository.deleteById(id);
            log.info("Product deleted: id={}", id);
        } finally {
            span.end();
        }
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> p) {
        return PageResponse.<ProductResponse>builder()
                .content(p.getContent().stream().map(this::toResponse).toList())
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .first(p.isFirst())
                .last(p.isLast())
                .build();
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
