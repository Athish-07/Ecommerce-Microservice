package com.example.order_service.controller;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest request,
                                        Authentication authentication) {

        String userEmail = authentication.getName(); // subject = email
        Order created = orderService.createOrder(userEmail, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {

        String userEmail = authentication.getName();
        List<Order> orders = orderService.getMyOrders(userEmail);

        return ResponseEntity.ok(orders);
    }
}
