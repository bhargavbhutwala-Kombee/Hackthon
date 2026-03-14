package com.kombee.orderly.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetry;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    public static final String INSTRUMENTATION_SCOPE = "com.kombee.orderly";

    @Bean
    public Tracer tracer() {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build();
        OpenTelemetry openTelemetry = OpenTelemetry.builder()
                .setTracerProvider(tracerProvider)
                .build();
        GlobalOpenTelemetry.set(openTelemetry);
        return openTelemetry.getTracer(INSTRUMENTATION_SCOPE, "1.0.0");
    }
}
