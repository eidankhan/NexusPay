package dev.nexus.app.identityservice;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserCredentialRepository repository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserCredentialRepository repository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(AuthRequest request) {
        if (repository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Username already taken!");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        UserCredential newUser = new UserCredential(request.username(), hashedPassword);
        repository.save(newUser);

        // Return structured JSON instead of a raw string
        return new AuthResponse(null, "User registered successfully!");
    }

    public AuthResponse login(AuthRequest request) {
        UserCredential user = repository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Invalid Credentials!"));

        if (passwordEncoder.matches(request.password(), user.getPassword())) {
            String token = jwtService.generateToken(user.getUsername());
            // Return the token inside our JSON container
            return new AuthResponse(token, "Login successful!");
        } else {
            throw new RuntimeException("Invalid Credentials!");
        }
    }
}