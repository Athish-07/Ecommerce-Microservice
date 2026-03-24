package com.example.order_service.controller;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.entity.Order;
import com.example.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                Authentication authentication) {

        String userEmail = authentication.getName(); // subject = email
        Order created = orderService.createOrder(userEmail, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userEmail = authentication.getName();
        Page<Order> orders = orderService.getMyOrders(userEmail, page, size);

        return ResponseEntity.ok(orders.map(OrderResponse::from));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrder(@PathVariable Long orderId, Authentication authentication) {
        String userEmail = authentication.getName();
        Order order = orderService.getMyOrder(orderId, userEmail);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
