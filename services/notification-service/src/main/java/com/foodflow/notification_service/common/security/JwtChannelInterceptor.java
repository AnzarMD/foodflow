package com.foodflow.notification_service.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Only validate JWT on CONNECT — not on every SUBSCRIBE or SEND.
            // Once the session is authenticated on CONNECT, Spring remembers
            // the principal for the lifetime of the WebSocket connection.
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT missing Authorization header — rejecting");
                throw new IllegalArgumentException("Missing Authorization header");
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.validateAndExtract(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                accessor.setUser(auth);
                // ↑ This is the critical line. Setting the user principal on the
                //   STOMP session is what makes convertAndSendToUser() work.
                //   Spring uses accessor.getUser().getName() (= userId) to route
                //   messages to /user/{userId}/queue/notifications.
                //   Without this, convertAndSendToUser would have no idea
                //   which WebSocket session to send to.

                log.info("WebSocket CONNECT authenticated for user: {}", userId);
            } catch (JwtException e) {
                log.warn("WebSocket CONNECT rejected — invalid JWT: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid JWT token");
            }
        }

        return message;
    }
}