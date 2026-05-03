package dev.nexus.app.paymentservice;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        String status,
        String message,
        String stripeId,
        UUID localTransactionId,
        BigDecimal amount,
        String currency
) {}
