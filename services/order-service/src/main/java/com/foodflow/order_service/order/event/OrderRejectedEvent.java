package com.foodflow.order_service.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRejectedEvent {
    private UUID orderId;
    private UUID restaurantId;
    private UUID customerId;
    private String reason;
}