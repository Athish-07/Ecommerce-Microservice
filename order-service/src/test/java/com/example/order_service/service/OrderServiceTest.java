package com.example.order_service.service;

import com.example.order_service.client.ProductClient;
import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderOutboxEvent;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.event.InventoryStockResultEvent;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.repository.OrderOutboxEventRepository;
import com.example.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderOutboxEventRepository orderOutboxEventRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderShouldCalculateTotalAndReturnPendingOrder() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(7L);
        request.setQuantity(3);

        when(productClient.getProduct(7L)).thenReturn(new ProductClient.ProductResponse(7L, "Phone", new BigDecimal("199.50"), 10));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(42L);
            }
            return order;
        });

        Order order = orderService.createOrder("user@example.com", request);

        assertThat(order.getId()).isEqualTo(42L);
        assertThat(order.getUserEmail()).isEqualTo("user@example.com");
        assertThat(order.getProductId()).isEqualTo(7L);
        assertThat(order.getQuantity()).isEqualTo(3);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("598.50");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderOutboxEventRepository).save(any(OrderOutboxEvent.class));
    }

    @Test
    void handleInventoryStockResultShouldMarkOrderCreated() {
        Order order = Order.builder()
                .id(9L)
                .userEmail("user@example.com")
                .productId(3L)
                .quantity(2)
                .totalAmount(new BigDecimal("250.00"))
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        when(orderOutboxEventRepository.findByOrderId(9L)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.handleInventoryStockResult(new InventoryStockResultEvent(9L, 3L, true, "ok"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void handleInventoryStockResultShouldCancelOrderOnFailure() {
        Order order = Order.builder()
                .id(10L)
                .userEmail("user@example.com")
                .productId(4L)
                .quantity(1)
                .totalAmount(new BigDecimal("100.00"))
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderOutboxEventRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.handleInventoryStockResult(new InventoryStockResultEvent(10L, 4L, false, "Insufficient stock"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void getMyOrderShouldReturnOrderForMatchingUser() {
        Order order = Order.builder()
                .id(15L)
                .userEmail("user@example.com")
                .productId(2L)
                .quantity(1)
                .totalAmount(new BigDecimal("99.00"))
                .status(OrderStatus.CREATED)
                .build();

        when(orderRepository.findByIdAndUserEmail(15L, "user@example.com")).thenReturn(Optional.of(order));

        Order result = orderService.getMyOrder(15L, "user@example.com");

        assertThat(result).isSameAs(order);
    }

    @Test
    void getMyOrderShouldRejectUnknownOrderForUser() {
        when(orderRepository.findByIdAndUserEmail(15L, "user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder(15L, "user@example.com"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found");
    }
}
