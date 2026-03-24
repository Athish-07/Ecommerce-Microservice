package com.example.order_service.service;

import com.example.order_service.client.ProductClient;
import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderOutboxEvent;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.entity.OutboxEventStatus;
import com.example.order_service.event.InventoryStockResultEvent;
import com.example.order_service.exception.OrderPersistenceException;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.repository.OrderOutboxEventRepository;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderOutboxEventRepository orderOutboxEventRepository;
    private final ProductClient productClient;

    @Transactional
    public Order createOrder(String userEmail, CreateOrderRequest request) {
        ProductClient.ProductResponse product = productClient.getProduct(request.getProductId());
        log.info("Creating order for user {} productId {} quantity {}", userEmail, request.getProductId(), request.getQuantity());

        Order order = Order.builder()
                .userEmail(userEmail)
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(product.price().multiply(BigDecimal.valueOf(request.getQuantity())))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order pendingOrder = orderRepository.save(order);
        log.info("Stored pending order for user {} productId {}", userEmail, request.getProductId());

        orderOutboxEventRepository.save(
                OrderOutboxEvent.builder()
                        .orderId(pendingOrder.getId())
                        .productId(request.getProductId())
                        .quantity(request.getQuantity())
                        .status(OutboxEventStatus.PENDING)
                        .publishAttempts(0)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        log.info("Stored outbox event for order {}", pendingOrder.getId());

        return pendingOrder;
    }

    public Page<Order> getMyOrders(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findByUserEmail(userEmail, pageable);
        log.debug("Resolved {} orders for user {}", orders.getNumberOfElements(), userEmail);
        return orders;
    }

    public Order getMyOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, userEmail)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        log.debug("Resolved order {} for user {}", orderId, userEmail);
        return order;
    }

    @Transactional
    public void handleInventoryStockResult(InventoryStockResultEvent event) {
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderPersistenceException("Order not found for inventory result event", null));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Ignoring inventory result for order {} because status is {}", order.getId(), order.getStatus());
            return;
        }

        if (event.success()) {
            order.setStatus(OrderStatus.CREATED);
            orderRepository.save(order);
            orderOutboxEventRepository.findByOrderId(order.getId()).ifPresent(orderOutboxEventRepository::delete);
            log.info("Created order {} after inventory confirmed stock reduction", order.getId());
            return;
        }

        cancelOrder(order);
        log.warn("Cancelled order {} after inventory rejection: {}", order.getId(), event.message());
    }

    private void cancelOrder(Order order) {
        try {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            orderOutboxEventRepository.findByOrderId(order.getId()).ifPresent(orderOutboxEventRepository::delete);
            log.warn("Cancelled order for user {} productId {}", order.getUserEmail(), order.getProductId());
        } catch (Exception ex) {
            log.error("Failed to cancel order for user {} productId {}", order.getUserEmail(), order.getProductId(), ex);
            throw new OrderPersistenceException(
                    "Order could not be finalized after the inventory step failed",
                    ex
            );
        }
    }
}
