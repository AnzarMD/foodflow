package com.foodflow.order_service.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodflow.order_service.common.config.RedisConfig;
import com.foodflow.order_service.common.config.RabbitMQConfig;
import com.foodflow.order_service.order.entity.Order;
import com.foodflow.order_service.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    // ObjectMapper is auto-configured by Spring Boot — no need to declare a bean.
    // We use it to manually serialize the Redis payload to a JSON string.

    @RabbitListener(queues = "#{T(com.foodflow.order_service.common.config.RabbitMQConfig).ORDER_QUEUE}")
    @Transactional
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        log.info("Received OrderAccepted event for order: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresentOrElse(order -> {
            if (order.getStatus() != Order.OrderStatus.PENDING) {
                // Guard: only update if still PENDING.
                // The order might have been cancelled by the customer
                // between the restaurant accepting and this event arriving.
                log.warn("Order {} is not PENDING (status: {}), skipping accept",
                        event.getOrderId(), order.getStatus());
                return;
            }
            order.setStatus(Order.OrderStatus.ACCEPTED);
            orderRepository.save(order);
            log.info("Order {} status updated to ACCEPTED", event.getOrderId());

            // Publish to Redis for Notification Service (Day 10)
            publishToRedis(event.getOrderId().toString(),
                    event.getCustomerId().toString(), "ACCEPTED");

        }, () -> log.warn("Order {} not found — skipping accept event", event.getOrderId()));
    }

    @RabbitListener(queues = "#{T(com.foodflow.order_service.common.config.RabbitMQConfig).ORDER_QUEUE}")
    @Transactional
    public void handleOrderRejected(OrderRejectedEvent event) {
        log.info("Received OrderRejected event for order: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresentOrElse(order -> {
            if (order.getStatus() != Order.OrderStatus.PENDING) {
                log.warn("Order {} is not PENDING (status: {}), skipping reject",
                        event.getOrderId(), order.getStatus());
                return;
            }
            order.setStatus(Order.OrderStatus.CANCELLED);
            // We use CANCELLED (not a separate REJECTED status) because from
            // the customer's perspective, the order didn't go through.
            orderRepository.save(order);
            log.info("Order {} status updated to CANCELLED", event.getOrderId());

            publishToRedis(event.getOrderId().toString(),
                    event.getCustomerId().toString(), "CANCELLED");

        }, () -> log.warn("Order {} not found — skipping reject event", event.getOrderId()));
    }

    private void publishToRedis(String orderId, String customerId, String status) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "customerId", customerId,
                    "status", status
            ));
            redisTemplate.convertAndSend(RedisConfig.ORDER_UPDATES_CHANNEL, payload);
            // convertAndSend on RedisTemplate = Redis PUBLISH command.
            // Fire-and-forget — no persistence, no acknowledgement.
            // Notification Service (subscribed to this channel) receives it instantly.
            log.info("Published order status update to Redis for order: {}", orderId);
        } catch (JsonProcessingException e) {
            // Non-critical — the order status is already updated in DB.
            // If Redis publish fails, the customer just won't get a real-time push.
            // They can still poll GET /api/v1/orders/{id} to see the updated status.
            log.error("Failed to publish Redis message for order {}: {}", orderId, e.getMessage());
        }
    }
}