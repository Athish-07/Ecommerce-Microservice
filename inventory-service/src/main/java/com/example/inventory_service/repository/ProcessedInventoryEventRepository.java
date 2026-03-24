package com.example.inventory_service.repository;

import com.example.inventory_service.entity.ProcessedInventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedInventoryEventRepository extends JpaRepository<ProcessedInventoryEvent, Long> {
    Optional<ProcessedInventoryEvent> findByOrderId(Long orderId);
}
