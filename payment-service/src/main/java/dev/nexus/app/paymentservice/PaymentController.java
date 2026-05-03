package dev.nexus.app.paymentservice;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProcessorService paymentService;

    // Spring hands the Controller the Service automatically
    public PaymentController(PaymentProcessorService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charge")
    public String chargeCard(@RequestBody PaymentRequest request) {
        // Hand the JSON request to our Service logic
        return paymentService.processPayment(request);
    }
}