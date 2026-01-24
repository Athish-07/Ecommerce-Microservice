package com.example.order_service.service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.client.NotificationClient;
import com.example.order_service.client.PaymentClient;
import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.dto.NotificationRequest;
import com.example.order_service.dto.PaymentRequest;
import com.example.order_service.dto.PaymentResponse;
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

    private final NotificationClient notificationClient;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;


    public Order createOrder(String userEmail, CreateOrderRequest request) {

        // 1) Reduce stock using Inventory Service
        inventoryClient.reduceStock(
                new InventoryClient.ReduceStockRequest(
                        request.getProductId(),
                        request.getQuantity()
                )
        );

        // 2) Call Payment Service
        PaymentResponse paymentResponse = paymentClient.pay(
                new PaymentRequest(request.getTotalAmount())
        );

        if (paymentResponse == null || paymentResponse.getStatus() == null ||
                !"SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
            throw new RuntimeException("Payment failed!");
        }

        // 3) Save Order
        Order order = Order.builder()
                .userEmail(userEmail)
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 4) Send notification (after order saved)
        notificationClient.send(
                new NotificationRequest("Order placed successfully! Order Amount: " + request.getTotalAmount())
        );

        return savedOrder;
    }


    public List<Order> getMyOrders(String userEmail) {
        return orderRepository.findByUserEmail(userEmail);
    }
}
