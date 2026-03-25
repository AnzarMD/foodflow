package com.foodflow.restaurant_service.restaurant.repository;

import com.foodflow.restaurant_service.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    Page<Restaurant> findByActiveTrue(Pageable pageable);

    List<Restaurant> findByOwnerId(UUID ownerId);

    Optional<Restaurant> findByIdAndOwnerId(UUID id, UUID ownerId);
}
