package com.kombee.orderly.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Injects configurable anomalies for observability demo: latency and random errors.
 * Toggle via application.yml: anomaly.enabled, anomaly.latency-ms, anomaly.error-probability.
 */
@Component
public class AnomalyInjector {

    private static final Logger log = LoggerFactory.getLogger(AnomalyInjector.class);

    @Value("${anomaly.enabled:false}")
    private boolean enabled;

    @Value("${anomaly.latency-ms:500}")
    private long latencyMs;

    @Value("${anomaly.error-probability:0.0}")
    private double errorProbability;

    @Value("${anomaly.error-endpoints:/api/products,/api/orders}")
    private String errorEndpointsConfig;

    public void maybeInject(String path) {
        if (!enabled) {
            return;
        }
        List<String> errorEndpoints = Arrays.asList(errorEndpointsConfig.split(","));
        if (latencyMs > 0) {
            try {
                log.warn("Anomaly: injecting latency {} ms on path {}", latencyMs, path);
                Thread.sleep(latencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Anomaly latency sleep interrupted");
            }
        }
        if (errorProbability > 0 && errorEndpoints.stream().anyMatch(path::startsWith)) {
            if (ThreadLocalRandom.current().nextDouble() < errorProbability) {
                log.error("Anomaly: injecting random 500 for path {}", path);
                throw new RuntimeException("Anomaly: simulated server error for observability demo");
            }
        }
    }
}
