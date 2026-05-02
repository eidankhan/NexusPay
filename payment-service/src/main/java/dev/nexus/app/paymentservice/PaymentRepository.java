package dev.nexus.app.paymentservice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

// JpaRepository gives us built-in methods like .save(), .findAll(), and .findById()
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
