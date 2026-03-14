package com.kombee.orderly.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

/**
 * Custom business and observability metrics for Grafana dashboards.
 */
@Component
public class ObservabilityMetrics {

    private final Counter loginAttemptsTotal;
    private final Counter loginFailuresTotal;
    private final Counter registerTotal;
    private final Counter registerFailuresTotal;
    private final Counter ordersCreatedTotal;
    private final Counter productsCreatedTotal;
    private final Counter validationFailuresTotal;

    public ObservabilityMetrics(MeterRegistry registry) {
        loginAttemptsTotal = registry.counter("auth_login_attempts_total");
        loginFailuresTotal = registry.counter("auth_login_failures_total");
        registerTotal = registry.counter("auth_register_total");
        registerFailuresTotal = registry.counter("auth_register_failures_total");
        ordersCreatedTotal = registry.counter("orders_created_total");
        productsCreatedTotal = registry.counter("products_created_total");
        validationFailuresTotal = registry.counter("validation_failures_total", Tags.of("type", "request"));
    }

    public void loginAttempt() {
        loginAttemptsTotal.increment();
    }

    public void loginFailure() {
        loginFailuresTotal.increment();
    }

    public void loginSuccess() {
        // optional: auth_login_success_total if needed
    }

    public void registerSuccess() {
        registerTotal.increment();
    }

    public void registerFailure() {
        registerFailuresTotal.increment();
    }

    public void orderCreated() {
        ordersCreatedTotal.increment();
    }

    public void productCreated() {
        productsCreatedTotal.increment();
    }

    public void validationFailure() {
        validationFailuresTotal.increment();
    }
}
