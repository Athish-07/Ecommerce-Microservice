package com.example.order_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String INVENTORY_STOCK_REQUEST_QUEUE = "inventory.stock.request";
    public static final String INVENTORY_STOCK_REQUEST_DLQ = "inventory.stock.request.dlq";
    public static final String INVENTORY_STOCK_RESULT_QUEUE = "order.inventory.result";
    public static final String INVENTORY_STOCK_RESULT_DLQ = "order.inventory.result.dlq";
    public static final String INVENTORY_STOCK_REQUEST_ROUTING_KEY = "inventory.stock.request";
    public static final String INVENTORY_STOCK_RESULT_ROUTING_KEY = "inventory.stock.result";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue inventoryStockRequestQueue() {
        return QueueBuilder.durable(INVENTORY_STOCK_REQUEST_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(INVENTORY_STOCK_REQUEST_DLQ)
                .build();
    }

    @Bean
    public Queue inventoryStockRequestDlq() {
        return QueueBuilder.durable(INVENTORY_STOCK_REQUEST_DLQ).build();
    }

    @Bean
    public Queue inventoryStockResultQueue() {
        return QueueBuilder.durable(INVENTORY_STOCK_RESULT_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(INVENTORY_STOCK_RESULT_DLQ)
                .build();
    }

    @Bean
    public Queue inventoryStockResultDlq() {
        return QueueBuilder.durable(INVENTORY_STOCK_RESULT_DLQ).build();
    }

    @Bean
    public Binding inventoryStockRequestBinding(Queue inventoryStockRequestQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(inventoryStockRequestQueue)
                .to(orderExchange)
                .with(INVENTORY_STOCK_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryStockResultBinding(Queue inventoryStockResultQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(inventoryStockResultQueue)
                .to(orderExchange)
                .with(INVENTORY_STOCK_RESULT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
