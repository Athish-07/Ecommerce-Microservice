package com.example.payment_service.controller;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request,
                                               Authentication authentication) {

        String userEmail = authentication.getName();

        // for now simulate payment success
        return ResponseEntity.ok(
                new PaymentResponse(
                        "Payment successful for user: " + userEmail,
                        "SUCCESS"
                )
        );
    }
}
