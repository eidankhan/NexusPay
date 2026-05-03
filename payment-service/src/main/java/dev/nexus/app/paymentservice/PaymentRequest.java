package dev.nexus.app.paymentservice;

import java.math.BigDecimal;

// A 'Record' is a modern Java feature that creates a simple, immutable data carrier.
// It automatically creates the getters and setters for us!
public record PaymentRequest(
        String email,
        BigDecimal amount,
        String currency
) {}