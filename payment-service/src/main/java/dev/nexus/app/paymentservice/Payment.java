package dev.nexus.app.paymentservice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Entity // Tells Hibernate: "Make a PostgreSQL table out of this class"
@Table(name = "payments") // Names the table 'payments'
@Data // Lombok magic: Auto-generates Getters and Setters for us
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id // This is the Primary Key
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-generate a random UUID for each payment
    private UUID id;

    private String userEmail;
    private BigDecimal amount;
    private String currency;
    private String status; // e.g., "PENDING", "SUCCESS", "FAILED"
}