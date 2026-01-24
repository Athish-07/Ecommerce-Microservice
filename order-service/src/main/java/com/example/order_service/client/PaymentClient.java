package com.example.order_service.client;

import com.example.order_service.dto.PaymentRequest;
import com.example.order_service.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/payments/pay")
    PaymentResponse pay(@RequestBody PaymentRequest request);
}
