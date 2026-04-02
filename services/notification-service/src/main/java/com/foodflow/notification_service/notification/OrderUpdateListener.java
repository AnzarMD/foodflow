package com.foodflow.notification_service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderUpdateListener implements MessageListener {
    // ↑ Implements Spring Data Redis MessageListener interface.
    //   RedisMessageListenerContainer calls onMessage() when a Redis
    //   pub/sub message arrives on the "order-updates" channel.

    private final SimpMessagingTemplate messagingTemplate;
    // ↑ Spring's WebSocket messaging abstraction.
    //   Auto-configured by @EnableWebSocketMessageBroker.
    //   Used to send messages to connected STOMP clients.

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        // ↑ The raw Redis message body — this is the JSON string
        //   Order Service published: {"orderId":"...","customerId":"...","status":"..."}

        log.info("Received Redis message on order-updates: {}", payload);

        try {
            // Extract customerId from the JSON payload to know who to notify.
            // We parse manually to avoid adding a JSON dependency just for this.
            String customerId = extractField(payload, "customerId");

            if (customerId == null) {
                log.warn("No customerId in Redis message — cannot route: {}", payload);
                return;
            }

            messagingTemplate.convertAndSendToUser(
                    customerId,                  // ← which user to send to
                    "/queue/notifications",      // ← their subscription destination
                    payload                      // ← the JSON payload as-is
            );
            // ↑ Spring looks up all WebSocket sessions whose principal name = customerId
            //   and sends the payload to each one at /user/{customerId}/queue/notifications.
            //   The browser subscribed to /user/queue/notifications receives it.

            log.info("Pushed notification to customer: {}", customerId);

        } catch (Exception e) {
            log.error("Error processing Redis message: {}", e.getMessage(), e);
        }
    }

    private String extractField(String json, String fieldName) {
        // Simple string-based JSON field extraction.
        // Avoids pulling in ObjectMapper just for this one operation.
        String key = "\"" + fieldName + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}