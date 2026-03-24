package com.example.inventory_service.messaging;

import com.example.inventory_service.config.RabbitMqConfig;
import com.example.inventory_service.dto.ReduceStockRequest;
import com.example.inventory_service.entity.ProcessedInventoryEvent;
import com.example.inventory_service.event.InventoryStockRequestEvent;
import com.example.inventory_service.event.InventoryStockResultEvent;
import com.example.inventory_service.exception.InsufficientStockException;
import com.example.inventory_service.exception.InventoryNotFoundException;
import com.example.inventory_service.repository.ProcessedInventoryEventRepository;
import com.example.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class InventoryStockRequestListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryStockRequestListener.class);

    private final InventoryService inventoryService;
    private final InventoryEventPublisher inventoryEventPublisher;
    private final ProcessedInventoryEventRepository processedInventoryEventRepository;

    @RabbitListener(queues = RabbitMqConfig.INVENTORY_STOCK_REQUEST_QUEUE)
    @Transactional
    public void handleInventoryStockRequest(InventoryStockRequestEvent event) {
        ProcessedInventoryEvent existingEvent = processedInventoryEventRepository.findByOrderId(event.orderId()).orElse(null);
        if (existingEvent != null) {
            inventoryEventPublisher.publishInventoryStockResult(
                    new InventoryStockResultEvent(
                            existingEvent.getOrderId(),
                            existingEvent.getProductId(),
                            existingEvent.getSuccess(),
                            existingEvent.getMessage()
                    )
            );
            log.info("Replayed existing inventory result for duplicate order event {}", event.orderId());
            return;
        }

        ReduceStockRequest request = new ReduceStockRequest();
        request.setProductId(event.productId());
        request.setQuantity(event.quantity());

        try {
            inventoryService.reduceStock(request);
            publishAndRecord(event, true, "Stock reduced successfully");
            log.info("Processed async inventory stock request for order {}", event.orderId());
        } catch (InventoryNotFoundException | InsufficientStockException ex) {
            publishAndRecord(event, false, ex.getMessage());
            log.warn("Inventory request rejected for order {}: {}", event.orderId(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Async inventory stock request failed for order {}", event.orderId(), ex);
            throw ex;
        }
    }

    private void publishAndRecord(InventoryStockRequestEvent event, boolean success, String message) {
        processedInventoryEventRepository.save(
                ProcessedInventoryEvent.builder()
                        .orderId(event.orderId())
                        .productId(event.productId())
                        .quantity(event.quantity())
                        .success(success)
                        .message(message)
                        .processedAt(LocalDateTime.now())
                        .build()
        );

        inventoryEventPublisher.publishInventoryStockResult(
                new InventoryStockResultEvent(event.orderId(), event.productId(), success, message)
        );
    }
}
