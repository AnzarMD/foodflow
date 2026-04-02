package com.foodflow.notification_service.common.config;

import com.foodflow.notification_service.common.security.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
// ↑ Enables STOMP over WebSocket. Without this, Spring only supports
//   raw WebSocket — no subscriptions, no user destinations, no routing.
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                // ↑ The URL the browser connects to: ws://localhost:8083/ws
                //   setAllowedOriginPatterns("*") allows any origin — fine for dev.
                //   In production, restrict this to your frontend domain.
                .withSockJS();
        // ↑ Adds SockJS fallback. The browser tries WebSocket first;
        //   if blocked (corporate firewalls etc.), falls back to polling.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        // ↑ Spring's in-memory message broker handles these prefixes.
        //   /queue = point-to-point (one user), /topic = broadcast (all users).
        //   We use /queue for order status updates (customer-specific).

        registry.setApplicationDestinationPrefixes("/app");
        // ↑ Messages from browser to server start with /app.
        //   We don't have any browser→server messages today but this is standard.

        registry.setUserDestinationPrefix("/user");
        // ↑ Enables user-specific destinations.
        //   convertAndSendToUser("userId", "/queue/notifications", msg)
        //   internally becomes /user/{userId}/queue/notifications.
        //   Only the browser session with that userId principal receives it.
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
        // ↑ Every inbound STOMP frame (CONNECT, SUBSCRIBE, SEND) passes through
        //   this interceptor. We use it to validate the JWT on CONNECT
        //   and set the user principal so Spring knows who this session belongs to.
    }
}