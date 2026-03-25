package com.foodflow.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private final SecretKey secretKey;

    // Paths that bypass JWT validation entirely
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    );

    // Public GET paths — no token needed to browse restaurants/menus
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/v1/restaurants"
    );

    public JwtAuthGatewayFilter(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        // Skip JWT validation for public paths
        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        // Extract token from Authorization header
        String token = extractToken(request);
        if (token == null) {
            log.warn("Missing JWT for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // Inject user info as headers for downstream services
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path, String method) {
        // Always public
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        // Public only for GET requests (browsing restaurants/menus)
        if ("GET".equals(method) && PUBLIC_GET_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get("Authorization");
        if (headers != null && !headers.isEmpty()) {
            String header = headers.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -1; // Run before all other filters
    }
}