package com.foodflow.order_service.order.service;

import com.foodflow.order_service.auth.entity.User;
import com.foodflow.order_service.auth.repository.UserRepository;
import com.foodflow.order_service.common.exception.ResourceNotFoundException;
import com.foodflow.order_service.order.dto.*;
import com.foodflow.order_service.order.entity.Order;
import com.foodflow.order_service.order.entity.OrderItem;
import com.foodflow.order_service.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.foodflow.order_service.order.event.OrderPlacedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.foodflow.order_service.common.config.RabbitMQConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;


    private UUID getCurrentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        UUID customerId = getCurrentUserId();
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .menuItemId(i.getMenuItemId())
                        .name(i.getName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customer(customer)
                .restaurantId(request.getRestaurantId())
                .restaurantName(request.getRestaurantName())
                .deliveryAddress(request.getDeliveryAddress())
                .totalAmount(total)
                .status(Order.OrderStatus.PENDING)
                .build();

        items.forEach(item -> item.setOrder(order));
        order.getItems().addAll(items);

        Order saved = orderRepository.save(order);
        log.info("Order placed: {} by customer: {}", saved.getId(), customerId);

        // Publish OrderPlaced event to RabbitMQ
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(saved.getId())
                .customerId(customerId)
                .restaurantId(saved.getRestaurantId())
                .restaurantName(saved.getRestaurantName())
                .deliveryAddress(saved.getDeliveryAddress())
                .totalAmount(saved.getTotalAmount())
                .items(saved.getItems().stream()
                        .map(i -> OrderPlacedEvent.OrderItemDetail.builder()
                                .menuItemId(i.getMenuItemId())
                                .name(i.getName())
                                .unitPrice(i.getUnitPrice())
                                .quantity(i.getQuantity())
                                .build())
                        .toList())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,           // ← which exchange
                RabbitMQConfig.ROUTING_KEY_ORDER_PLACED,  // ← routing key
                event                              // ← payload (serialized to JSON automatically)
        );
        // ↑ This call is fire-and-forget from the HTTP request perspective.
        //   The order is already saved to DB above. Even if RabbitMQ is temporarily
        //   unreachable, the order exists. The event publish failure would throw
        //   an exception here — we'll handle that more robustly in production
        //   with publisher confirms, but for now this is fine for development.

        log.info("OrderPlaced event published for order: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        // Security: customers can only see their own orders
        if (!order.getCustomer().getId().equals(getCurrentUserId())) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        return orderRepository.findByCustomerId(getCurrentUserId(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getCustomer().getId().equals(getCurrentUserId())) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemResponse> itemResponses = o.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .id(i.getId())
                        .menuItemId(i.getMenuItemId())
                        .name(i.getName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(o.getId())
                .customerId(o.getCustomer().getId())
                .restaurantId(o.getRestaurantId())
                .restaurantName(o.getRestaurantName())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .deliveryAddress(o.getDeliveryAddress())
                .items(itemResponses)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}