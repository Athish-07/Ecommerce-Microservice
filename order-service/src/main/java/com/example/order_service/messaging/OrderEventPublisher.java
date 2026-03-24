package com.example.order_service.messaging;

import com.example.order_service.config.RabbitMqConfig;
import com.example.order_service.event.InventoryStockRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishInventoryStockRequest(InventoryStockRequestEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_EXCHANGE,
                RabbitMqConfig.INVENTORY_STOCK_REQUEST_ROUTING_KEY,
                event
        );
    }
}
