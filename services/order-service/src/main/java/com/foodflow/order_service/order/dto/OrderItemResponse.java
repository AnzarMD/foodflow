package com.foodflow.order_service.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponse {
    private UUID id;
    private UUID menuItemId;
    private String name;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;    // unitPrice × quantity — computed on the fly
}