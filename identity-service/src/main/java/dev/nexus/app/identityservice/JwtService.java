package dev.nexus.app.identityservice;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username) // Who is this token for?
                .issuedAt(new Date(System.currentTimeMillis())) // When was it made?
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30)) // Expires in 30 mins
                .signWith(getSigningKey()) // The cryptographic signature
                .compact(); // Build it into a string
    }

    // Helper method to convert our Base64 string into a true Cryptographic Key
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}