package com.kombee.orderly.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Starts a request span and puts OpenTelemetry trace ID and span ID into MDC
 * so logs can be correlated and filtered by trace ID in Loki.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdMdcFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";

    private final Tracer tracer;

    public TraceIdMdcFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        Span span = tracer.spanBuilder("http.request")
                .setAttribute("http.method", request.getMethod())
                .setAttribute("http.url", request.getRequestURI())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            if (span.getSpanContext().isValid()) {
                MDC.put(TRACE_ID, span.getSpanContext().getTraceId());
                MDC.put(SPAN_ID, span.getSpanContext().getSpanId());
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
            span.end();
        }
    }
}
