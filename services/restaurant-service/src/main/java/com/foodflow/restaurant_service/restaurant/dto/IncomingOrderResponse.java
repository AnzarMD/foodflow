package com.foodflow.restaurant_service.restaurant.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class IncomingOrderResponse {
    private UUID id;                // local incoming_orders row ID
    private UUID orderId;           // Order Service's order ID
    private UUID customerId;
    private UUID restaurantId;
    private String restaurantName;
    private String status;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
