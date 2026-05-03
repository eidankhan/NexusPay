package dev.nexus.app.paymentservice;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service // Tells Spring this is our business logic layer
public class PaymentProcessorService {

    private final PaymentRepository paymentRepository;

    // We pull the secret key right out of our application.yml!
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    // Constructor Injection (Spring automatically gives us the Database tool)
    public PaymentProcessorService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        // 1. Create a "PENDING" record in our database
        Payment payment = new Payment(null, request.email(), request.amount(), request.currency(), "PENDING");
        payment = paymentRepository.save(payment);

        try {
            Stripe.apiKey = stripeSecretKey;
            long amountInCents = request.amount().multiply(new BigDecimal(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.currency())
                    .setPaymentMethod("pm_card_visa")
                    .setConfirm(true)
                    .setReturnUrl("http://localhost:8080/success")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // 2. Success Case
            payment.setStatus("SUCCESS");
            paymentRepository.save(payment);

            return new PaymentResponse(
                    "SUCCESS",
                    "Payment processed successfully",
                    intent.getId(),
                    payment.getId(),
                    request.amount(),
                    request.currency()
            );

        } catch (Exception e) {
            // 3. Failure Case
            payment.setStatus("FAILED");
            paymentRepository.save(payment);

            return new PaymentResponse(
                    "FAILED",
                    "Payment failed: " + e.getMessage(),
                    null,
                    payment.getId(),
                    request.amount(),
                    request.currency()
            );
        }
    }
}