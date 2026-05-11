package dev.nexus.app.paymentservice;

import java.math.BigDecimal;

public record PaymentEvent(
        String transactionId,
        String customerEmail,
        BigDecimal amount,
        String currency,
        String status
) {}