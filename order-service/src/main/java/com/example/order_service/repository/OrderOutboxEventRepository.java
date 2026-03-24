package com.example.order_service.repository;

import com.example.order_service.entity.OrderOutboxEvent;
import com.example.order_service.entity.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEvent, Long> {
    List<OrderOutboxEvent> findByStatusInOrderByCreatedAtAsc(Collection<OutboxEventStatus> statuses, Pageable pageable);
    Optional<OrderOutboxEvent> findByOrderId(Long orderId);
}
