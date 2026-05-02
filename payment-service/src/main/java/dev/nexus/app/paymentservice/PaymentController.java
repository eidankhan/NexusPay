package dev.nexus.app.paymentservice;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @PostMapping("/charge")
    public String chargeCard() {
        // Later, we will write the code to save to PostgreSQL and call Stripe here!
        return "SUCCESS! The Payment Service received your request.";
    }
}