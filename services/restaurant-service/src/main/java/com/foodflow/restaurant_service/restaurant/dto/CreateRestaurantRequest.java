package com.foodflow.restaurant_service.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    private String name;

    private String address;
    private String cuisineType;
    private String description;
}
