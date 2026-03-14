package com.kombee.orderly.service;

import com.kombee.orderly.dto.common.PageResponse;
import com.kombee.orderly.dto.order.OrderItemRequest;
import com.kombee.orderly.dto.order.OrderItemResponse;
import com.kombee.orderly.dto.order.OrderRequest;
import com.kombee.orderly.dto.order.OrderResponse;
import com.kombee.orderly.entity.Order;
import com.kombee.orderly.entity.OrderItem;
import com.kombee.orderly.entity.Product;
import com.kombee.orderly.entity.User;
import com.kombee.orderly.dto.order.OrderStatusUpdateRequest;
import com.kombee.orderly.exception.ResourceNotFoundException;
import com.kombee.orderly.exception.ValidationException;
import com.kombee.orderly.metrics.ObservabilityMetrics;
import com.kombee.orderly.repository.OrderRepository;
import com.kombee.orderly.repository.ProductRepository;
import com.kombee.orderly.repository.UserRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final Tracer tracer;
    private final AnomalyInjector anomalyInjector;
    private final ObservabilityMetrics metrics;

    @Transactional
    public OrderResponse create(Long userId, OrderRequest request) {
        Span span = tracer.spanBuilder("order.create").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/orders");
            span.setAttribute("userId", userId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            List<OrderItem> items = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItemRequest itemReq : request.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));
                if (product.getStockQuantity() < itemReq.getQuantity()) {
                    log.warn("Order validation failed: insufficient stock for productId={}", product.getId());
                    throw new ValidationException("Insufficient stock for product: " + product.getName());
                }
                BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                total = total.add(lineTotal);
                OrderItem item = OrderItem.builder()
                        .product(product)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(product.getPrice())
                        .lineTotal(lineTotal)
                        .build();
                items.add(item);
            }
            Order order = Order.builder()
                    .user(user)
                    .status(Order.OrderStatus.PENDING)
                    .totalAmount(total)
                    .build();
            order = orderRepository.save(order);
            for (OrderItem item : items) {
                item.setOrder(order);
            }
            order.getItems().addAll(items);
            order = orderRepository.save(order);
            metrics.orderCreated();
            log.info("Order created: orderId={}, userId={}, total={}", order.getId(), userId, total);
            return toResponse(order);
        } finally {
            span.end();
        }
    }

    @Transactional
    public OrderResponse updateStatus(Long userId, Long orderId, Order.OrderStatus status) {
        Span span = tracer.spanBuilder("order.updateStatus").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/orders");
            span.setAttribute("orderId", orderId);
            span.setAttribute("status", status.name());
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
            if (!order.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Order not found: " + orderId);
            }
            order.setStatus(status);
            order = orderRepository.save(order);
            log.info("Order status updated: orderId={}, status={}", orderId, status);
            return toResponse(order);
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findByUser(Long userId, int page, int size, String status) {
        Span span = tracer.spanBuilder("order.findByUser").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/orders");
            span.setAttribute("userId", userId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Order.OrderStatus statusEnum = null;
            if (status != null && !status.isBlank()) {
                try {
                    statusEnum = Order.OrderStatus.valueOf(status.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid order status filter: {}", status);
                }
            }
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Order> p = statusEnum != null
                    ? orderRepository.findByUserAndStatus(user, statusEnum, pageable)
                    : orderRepository.findByUser(user, pageable);
            return PageResponse.<OrderResponse>builder()
                    .content(p.getContent().stream().map(this::toResponse).toList())
                    .page(p.getNumber())
                    .size(p.getSize())
                    .totalElements(p.getTotalElements())
                    .totalPages(p.getTotalPages())
                    .first(p.isFirst())
                    .last(p.isLast())
                    .build();
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long userId, Long orderId) {
        Span span = tracer.spanBuilder("order.getById").startSpan();
        try (var scope = span.makeCurrent()) {
            anomalyInjector.maybeInject("/api/orders");
            span.setAttribute("orderId", orderId);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
            if (!order.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Order not found: " + orderId);
            }
            return toResponse(order);
        } finally {
            span.end();
        }
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemResponse> itemResponses = o.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productSku(i.getProduct().getSku())
                        .productName(i.getProduct().getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();
        return OrderResponse.builder()
                .id(o.getId())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .items(itemResponses)
                .build();
    }
}
