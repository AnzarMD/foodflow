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
public class OrderAcceptedEvent {
    private UUID orderId;
    private UUID restaurantId;
    private UUID customerId;
    // Field names must exactly match what Restaurant Service published.
    // Jackson deserializes by field name — one mismatch = null field silently.
}