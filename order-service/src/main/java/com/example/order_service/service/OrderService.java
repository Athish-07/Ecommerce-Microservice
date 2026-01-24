package com.example.order_service.service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient; // ✅ added

    public Order createOrder(String userEmail, CreateOrderRequest request) {

        // ✅ Step 1: reduce stock
        String response = inventoryClient.reduceStock(
                new InventoryClient.ReduceStockRequest(
                        request.getProductId(),
                        request.getQuantity()
                )
        );

        if (!response.toLowerCase().contains("reduced")) {
            throw new RuntimeException("Order failed: " + response);
        }

        // ✅ Step 2: create order
        Order order = Order.builder()
                .userEmail(userEmail)
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        return orderRepository.save(order);
    }

    public List<Order> getMyOrders(String userEmail) {
        return orderRepository.findByUserEmail(userEmail);
    }
}
