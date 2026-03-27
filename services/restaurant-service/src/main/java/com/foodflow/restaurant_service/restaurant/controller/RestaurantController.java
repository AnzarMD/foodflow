package com.foodflow.restaurant_service.restaurant.controller;

import com.foodflow.restaurant_service.restaurant.dto.*;
import com.foodflow.restaurant_service.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // ── Existing endpoints (unchanged) ──────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<RestaurantResponse>> getAllRestaurants(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(restaurantService.getAllActiveRestaurants(pageable));
    }

    @GetMapping("/{id}/menu")
    public ResponseEntity<List<MenuItemResponse>> getMenu(@PathVariable UUID id) {
        return ResponseEntity.ok(restaurantService.getMenuByRestaurantId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.createRestaurant(request));
    }

    @PostMapping("/{id}/menu")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemResponse> addMenuItem(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(restaurantService.addMenuItem(id, request));
    }

    // ── Day 5: Incoming Order Management ────────────────────────────────────

    @GetMapping("/incoming-orders")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<List<IncomingOrderResponse>> getIncomingOrders() {
        return ResponseEntity.ok(restaurantService.getIncomingOrders());
    }

    @PatchMapping("/orders/{orderId}/accept")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<IncomingOrderResponse> acceptOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(restaurantService.acceptOrder(orderId));
    }

    @PatchMapping("/orders/{orderId}/reject")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<IncomingOrderResponse> rejectOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(restaurantService.rejectOrder(orderId));
    }

    //Check
//    @PostMapping("/test/incoming-order")
//    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
//    public ResponseEntity<IncomingOrderResponse> createTestOrder(
//            @Valid @RequestBody CreateIncomingOrderRequest request) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(restaurantService.createTestIncomingOrder(request));
//    }
}