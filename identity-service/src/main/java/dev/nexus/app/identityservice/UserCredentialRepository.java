package dev.nexus.app.identityservice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    // Spring Data JPA magically writes the SQL for this based on the method name!
    Optional<UserCredential> findByUsername(String username);
}