package com.foodflow.restaurant_service.restaurant.service;

import com.foodflow.restaurant_service.restaurant.dto.*;
import com.foodflow.restaurant_service.restaurant.entity.MenuItem;
import com.foodflow.restaurant_service.restaurant.entity.Restaurant;
import com.foodflow.restaurant_service.restaurant.repository.MenuItemRepository;
import com.foodflow.restaurant_service.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    // Extract the authenticated owner's ID from SecurityContext
    private UUID getCurrentUserId() {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return UUID.fromString(userId);
    }

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

    private RestaurantResponse toResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
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
}