package com.example.order_service.messaging;

import com.example.order_service.entity.OrderOutboxEvent;
import com.example.order_service.entity.OutboxEventStatus;
import com.example.order_service.event.InventoryStockRequestEvent;
import com.example.order_service.repository.OrderOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxPublisher.class);

    private final OrderOutboxEventRepository outboxEventRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Scheduled(fixedDelayString = "${order.outbox.publish-delay-ms:3000}")
    @Transactional
    public void publishPendingEvents() {
        List<OrderOutboxEvent> events = outboxEventRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED),
                PageRequest.of(0, 20)
        );

        for (OrderOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OrderOutboxEvent outboxEvent) {
        try {
            orderEventPublisher.publishInventoryStockRequest(
                    new InventoryStockRequestEvent(
                            outboxEvent.getOrderId(),
                            outboxEvent.getProductId(),
                            outboxEvent.getQuantity()
                    )
            );
            outboxEvent.setStatus(OutboxEventStatus.PUBLISHED);
            outboxEvent.setLastAttemptAt(LocalDateTime.now());
            outboxEvent.setPublishAttempts(outboxEvent.getPublishAttempts() + 1);
            outboxEvent.setLastError(null);
            log.info("Published inventory stock request from outbox for order {}", outboxEvent.getOrderId());
        } catch (Exception ex) {
            outboxEvent.setStatus(OutboxEventStatus.FAILED);
            outboxEvent.setLastAttemptAt(LocalDateTime.now());
            outboxEvent.setPublishAttempts(outboxEvent.getPublishAttempts() + 1);
            outboxEvent.setLastError(ex.getMessage());
            log.error("Failed to publish inventory stock request from outbox for order {}", outboxEvent.getOrderId(), ex);
        }
    }
}
