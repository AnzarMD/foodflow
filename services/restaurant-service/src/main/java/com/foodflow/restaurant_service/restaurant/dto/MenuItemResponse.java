package com.foodflow.restaurant_service.restaurant.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MenuItemResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private boolean available;
}