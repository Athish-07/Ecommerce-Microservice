package com.example.inventory_service.messaging;

import com.example.inventory_service.config.RabbitMqConfig;
import com.example.inventory_service.event.InventoryStockResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishInventoryStockResult(InventoryStockResultEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_EXCHANGE,
                RabbitMqConfig.INVENTORY_STOCK_RESULT_ROUTING_KEY,
                event
        );
    }
}
