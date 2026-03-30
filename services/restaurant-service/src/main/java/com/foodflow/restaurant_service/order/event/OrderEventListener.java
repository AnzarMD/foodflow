package com.foodflow.restaurant_service.order.event;

import com.foodflow.restaurant_service.restaurant.entity.IncomingOrder;
import com.foodflow.restaurant_service.restaurant.entity.Restaurant;
import com.foodflow.restaurant_service.restaurant.repository.IncomingOrderRepository;
import com.foodflow.restaurant_service.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final RestaurantRepository restaurantRepository;
    private final IncomingOrderRepository incomingOrderRepository;

    @RabbitListener(queues = "#{T(com.foodflow.restaurant_service.common.config.RabbitMQConfig).RESTAURANT_QUEUE}")
    // ↑ SpEL expression referencing the constant from RabbitMQConfig.
    //   Equivalent to @RabbitListener(queues = "restaurant-service-queue")
    //   but avoids hardcoding the string in two places.
    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlaced event for order: {} restaurant: {}",
                event.getOrderId(), event.getRestaurantId());

        // Idempotency check — if this event was already processed
        // (e.g. RabbitMQ redelivered it after a crash), skip it.
        if (incomingOrderRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("Duplicate OrderPlaced event for order: {} — skipping", event.getOrderId());
            return;
        }

        // Find the restaurant — it must exist in our DB
        Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId())
                .orElseGet(() -> {
                    log.warn("Restaurant {} not found in restaurant-service DB — skipping order {}",
                            event.getRestaurantId(), event.getOrderId());
                    return null;
                });

        if (restaurant == null) return;

        // Save the incoming order — this is what the owner sees in their dashboard
        IncomingOrder incomingOrder = IncomingOrder.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .restaurant(restaurant)
                .status(IncomingOrder.OrderStatus.PENDING)
                .totalAmount(event.getTotalAmount())
                .deliveryAddress(event.getDeliveryAddress())
                .build();

        incomingOrderRepository.save(incomingOrder);
        log.info("IncomingOrder saved for order: {}", event.getOrderId());
    }
}