package com.foodflow.restaurant_service.restaurant.service;

import com.foodflow.restaurant_service.restaurant.dto.*;
import com.foodflow.restaurant_service.restaurant.entity.IncomingOrder;
import com.foodflow.restaurant_service.restaurant.entity.MenuItem;
import com.foodflow.restaurant_service.restaurant.entity.Restaurant;
import com.foodflow.restaurant_service.restaurant.repository.IncomingOrderRepository;
import com.foodflow.restaurant_service.restaurant.repository.MenuItemRepository;
import com.foodflow.restaurant_service.restaurant.repository.RestaurantRepository;
import com.foodflow.restaurant_service.order.event.OrderAcceptedEvent;
import com.foodflow.restaurant_service.order.event.OrderRejectedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.foodflow.restaurant_service.common.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final IncomingOrderRepository incomingOrderRepository;
    private final RabbitTemplate rabbitTemplate;
// ← new

    private UUID getCurrentUserId() {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return UUID.fromString(userId);
    }

    // ── Existing methods (unchanged) ────────────────────────────────────────

    @Transactional
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
        UUID ownerId = getCurrentUserId();
        Restaurant restaurant = Restaurant.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .address(request.getAddress())
                .cuisineType(request.getCuisineType())
                .description(request.getDescription())
                .active(true)
                .build();
        return toResponse(restaurantRepository.save(restaurant));
    }

    public Page<RestaurantResponse> getAllActiveRestaurants(Pageable pageable) {
        return restaurantRepository.findByActiveTrue(pageable).map(this::toResponse);
    }

    public List<MenuItemResponse> getMenuByRestaurantId(UUID restaurantId) {
        return menuItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId)
                .stream().map(this::toMenuResponse).toList();
    }

    @Transactional
    public MenuItemResponse addMenuItem(UUID restaurantId, CreateMenuItemRequest request) {
        UUID ownerId = getCurrentUserId();
        Restaurant restaurant = restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Restaurant not found or you do not own it"));
        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .available(true)
                .build();
        return toMenuResponse(menuItemRepository.save(item));
    }

    // ── Day 5: Incoming Order Management ────────────────────────────────────

    /**
     * Returns all PENDING incoming orders across all restaurants owned by
     * the currently authenticated owner.
     */
    @Transactional(readOnly = true)
    public List<IncomingOrderResponse> getIncomingOrders() {
        UUID ownerId = getCurrentUserId();
        List<Restaurant> ownedRestaurants = restaurantRepository.findByOwnerId(ownerId);
        if (ownedRestaurants.isEmpty()) {
            return List.of();
        }
        List<UUID> restaurantIds = ownedRestaurants.stream()
                .map(Restaurant::getId)
                .toList();
        return incomingOrderRepository
                .findByRestaurant_IdInAndStatus(restaurantIds, IncomingOrder.OrderStatus.PENDING)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    /**
     * Accepts a PENDING order. orderId = the Order Service's order UUID.
     * Day 8 will add RabbitMQ publishing here.
     */
    @Transactional
    public IncomingOrderResponse acceptOrder(UUID orderId) {
        IncomingOrder order = getOrderForCurrentOwner(orderId);
        if (order.getStatus() != IncomingOrder.OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING orders can be accepted. Current status: " + order.getStatus());
        }
        order.setStatus(IncomingOrder.OrderStatus.ACCEPTED);
        IncomingOrder saved = incomingOrderRepository.save(order);

        // Publish OrderAccepted event — Order Service listens on Day 9
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ORDER_ACCEPTED,
                OrderAcceptedEvent.builder()
                        .orderId(order.getOrderId())        // ← Order Service's order ID
                        .restaurantId(order.getRestaurant().getId())
                        .customerId(order.getCustomerId())
                        .build()
        );
        log.info("OrderAccepted event published for order: {}", order.getOrderId());
        return toOrderResponse(saved);
    }

    /**
     * Rejects a PENDING order. orderId = the Order Service's order UUID.
     * Day 8 will add RabbitMQ publishing here.
     */
    @Transactional
    public IncomingOrderResponse rejectOrder(UUID orderId) {
        IncomingOrder order = getOrderForCurrentOwner(orderId);
        if (order.getStatus() != IncomingOrder.OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING orders can be rejected. Current status: " + order.getStatus());
        }
        order.setStatus(IncomingOrder.OrderStatus.REJECTED);
        IncomingOrder saved = incomingOrderRepository.save(order);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ORDER_REJECTED,
                OrderRejectedEvent.builder()
                        .orderId(order.getOrderId())
                        .restaurantId(order.getRestaurant().getId())
                        .customerId(order.getCustomerId())
                        .reason("Rejected by restaurant owner")
                        .build()
        );
        log.info("OrderRejected event published for order: {}", order.getOrderId());
        return toOrderResponse(saved);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Finds an incoming order by Order Service's orderId and verifies
     * that it belongs to a restaurant owned by the current user.
     */
    private IncomingOrder getOrderForCurrentOwner(UUID orderId) {
        UUID ownerId = getCurrentUserId();
        IncomingOrder order = incomingOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + orderId));
        if (!order.getRestaurant().getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException(
                    "Order not found: " + orderId);  // don't leak ownership info
        }
        return order;
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .ownerId(r.getOwnerId())
                .name(r.getName()) 
                .address(r.getAddress())
                .cuisineType(r.getCuisineType())
                .description(r.getDescription())
                .active(r.isActive())
                .averageRating(r.getAverageRating())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private MenuItemResponse toMenuResponse(MenuItem m) {
        return MenuItemResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .description(m.getDescription())
                .price(m.getPrice())
                .category(m.getCategory())
                .available(m.isAvailable())
                .build();
    }

    private IncomingOrderResponse toOrderResponse(IncomingOrder o) {
        return IncomingOrderResponse.builder()
                .id(o.getId())
                .orderId(o.getOrderId())
                .customerId(o.getCustomerId())
                .restaurantId(o.getRestaurant().getId())
                .restaurantName(o.getRestaurant().getName())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .deliveryAddress(o.getDeliveryAddress())
                .notes(o.getNotes())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}