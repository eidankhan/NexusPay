package dev.nexus.app.identityservice;

import org.springframework.web.bind.annotation.*;

@RestController // Tells Spring: "This class handles web requests!"
@RequestMapping("/api/auth") // Any request starting with /api/auth comes here
public class AuthController {

    // When a user sends a POST request to /api/auth/login, this method runs
    @PostMapping("/login")
    public String login() {
        // For now, we are just returning a fake "Wristband".
        // Later, we will connect this to PostgreSQL to check real passwords!
        return "SUCCESS! Here is your temporary VIP Wristband (JWT Token)";
    }
}