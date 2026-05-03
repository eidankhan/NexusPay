package dev.nexus.app.identityservice;

// Notice how clean this is! No getters/setters required.
// Jackson (Spring's JSON mapper) automatically turns this into { "token": "...", "message": "..." }
public record AuthResponse(String token, String message) {}