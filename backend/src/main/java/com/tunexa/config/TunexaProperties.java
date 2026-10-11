package com.tunexa.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tunexa")
public record TunexaProperties(Jwt jwt, Cors cors, Otp otp, Orders orders, boolean demoData) {

    public record Jwt(String secret, Duration ttl) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Otp(Duration ttl, int maxAttempts, int maxSendsPerHour, boolean exposeCode) {
    }

    public record Orders(
            long stampDuty,
            long codFee,
            long codMaxTotal,
            long freeShippingThreshold,
            Map<Integer, Long> shippingFeeByZone,
            Duration confirmationTimeout,
            int defaultCommissionBp) {
    }
}
