package com.foodflow.restaurant_service.restaurant.repository;

import com.foodflow.restaurant_service.restaurant.entity.IncomingOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomingOrderRepository extends JpaRepository<IncomingOrder, UUID> {

    // All orders for a set of restaurant IDs, filtered by status
    List<IncomingOrder> findByRestaurant_IdInAndStatus(
            List<UUID> restaurantIds, IncomingOrder.OrderStatus status);

    // All orders for a set of restaurant IDs (no status filter)
    List<IncomingOrder> findByRestaurant_IdIn(List<UUID> restaurantIds);

    // Lookup by the Order Service's order ID (used in accept/reject)
    Optional<IncomingOrder> findByOrderId(UUID orderId);
}
