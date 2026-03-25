package com.foodflow.restaurant_service.restaurant.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RestaurantResponse {
    private UUID id;
    private String name;
    private String address;
    private String cuisineType;
    private String description;
    private boolean active;
    private BigDecimal averageRating;
    private LocalDateTime createdAt;
}