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

    public String processPayment(PaymentRequest request) {
        // 1. Create a "PENDING" record in our database
        Payment payment = new Payment(null, request.email(), request.amount(), request.currency(), "PENDING");
        payment = paymentRepository.save(payment);

        try {
            // 2. Set up Stripe
            Stripe.apiKey = stripeSecretKey;

            // 3. Tell Stripe to charge the card (Stripe expects amounts in cents!)
            long amountInCents = request.amount().multiply(new BigDecimal(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.currency())
                    .setPaymentMethod("pm_card_visa") // <-- STRIPE'S MAGIC TEST CARD
                    .setConfirm(true)                 // <-- TELLS STRIPE TO CHARGE IT NOW
                    .setReturnUrl("http://localhost:8080/success") // <-- Dummy URL for redirects
                    .build();

            // 4. Send the request to Stripe over the internet
            PaymentIntent intent = PaymentIntent.create(params);

            // 5. If Stripe says yes, update our database to SUCCESS!
            payment.setStatus("SUCCESS - Stripe ID: " + intent.getId());
            paymentRepository.save(payment);

            return "Payment Successful! Receipt sent to " + request.email();

        } catch (Exception e) {
            // 6. If Stripe fails (or the network drops), update our database to FAILED
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            return "Payment Failed: " + e.getMessage();
        }
    }
}