package com.foodflow.order_service.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            // No token — let the request continue
            // Spring Security will reject it if the endpoint requires auth
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            // Only set authentication if not already set (prevents double processing)
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Create the authentication object Spring Security understands
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                                        // principal (who they are)
                                null,                                          // credentials (not needed — JWT already verified)
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)) // authorities
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // Store in SecurityContext — this is what makes the user "authenticated"
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (JwtException e) {
            // Invalid or expired token — clear context and continue
            // Spring Security will reject the request if auth is required
            log.warn("Invalid JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
}
