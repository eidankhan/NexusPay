package dev.nexus.app.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }
    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {

            // 1 & 2. Extract the Authorization header (Returns null if missing or empty)
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // 3. Check if the header is completely missing, OR if it doesn't start with "Bearer "
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            // 4. Strip the "Bearer " prefix so we just have the raw token string
            String token = authHeader.substring(7);

            // 5. Verify the mathematical signature
            try {
                jwtUtil.validateToken(token);
            } catch (Exception e) {
                // If it's expired or forged, drop the connection immediately
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            // 6. If the wristband is valid, let them pass to the Payment Service!
            return chain.filter(exchange);
        });
    }

    // Helper method for WebFlux to instantly return a 401 response without blocking threads
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    public static class Config {
        // We can leave this empty. It's required by the Abstract class for advanced property binding.
    }
}