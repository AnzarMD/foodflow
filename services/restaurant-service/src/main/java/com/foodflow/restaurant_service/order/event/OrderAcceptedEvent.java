package com.foodflow.restaurant_service.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAcceptedEvent {
    private UUID orderId;
    private UUID restaurantId;
    private UUID customerId;
    // Order Service will use orderId to find and update the order status.
    // We don't need to send all order details — just enough to identify it.
}