package com.foodflow.restaurant_service.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private UUID orderId;
    private UUID customerId;
    private UUID restaurantId;
    private String restaurantName;
    private String deliveryAddress;
    private BigDecimal totalAmount;
    private List<OrderItemDetail> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDetail {
        private UUID menuItemId;
        private String name;
        private BigDecimal unitPrice;
        private int quantity;
    }
}