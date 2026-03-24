package com.example.order_service.messaging;

import com.example.order_service.config.RabbitMqConfig;
import com.example.order_service.event.InventoryStockResultEvent;
import com.example.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryStockResultListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMqConfig.INVENTORY_STOCK_RESULT_QUEUE)
    public void handleInventoryStockResult(InventoryStockResultEvent event) {
        orderService.handleInventoryStockResult(event);
    }
}
