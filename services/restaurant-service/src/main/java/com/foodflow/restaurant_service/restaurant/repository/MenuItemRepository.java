package com.foodflow.restaurant_service.restaurant.repository;

import com.foodflow.restaurant_service.restaurant.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurantIdAndAvailableTrue(UUID restaurantId);

    Optional<MenuItem> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}
