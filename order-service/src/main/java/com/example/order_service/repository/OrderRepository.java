package com.example.order_service.repository;

import com.example.order_service.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserEmail(String userEmail);
    Page<Order> findByUserEmail(String userEmail, Pageable pageable);
    java.util.Optional<Order> findByIdAndUserEmail(Long id, String userEmail);
}
