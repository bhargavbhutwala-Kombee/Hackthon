package com.kombee.orderly.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    public static final String INSTRUMENTATION_SCOPE = "com.kombee.orderly";

    @Bean
    public Tracer tracer(
            @Value("${OTEL_EXPORTER_OTLP_ENDPOINT:}") String otlpEndpoint) {
        var builder = SdkTracerProvider.builder();
        if (otlpEndpoint != null && !otlpEndpoint.isBlank()) {
            SpanExporter exporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(otlpEndpoint.trim().replaceAll("/$", "") + "/v1/traces")
                    .build();
            builder.addSpanProcessor(BatchSpanProcessor.builder(exporter).build());
        }
        SdkTracerProvider tracerProvider = builder.build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        GlobalOpenTelemetry.set(openTelemetry);
        return openTelemetry.getTracer(INSTRUMENTATION_SCOPE, "1.0.0");
    }
}
