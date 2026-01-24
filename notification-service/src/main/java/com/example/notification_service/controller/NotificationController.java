package com.example.notification_service.controller;

import com.example.notification_service.dto.NotificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @PostMapping("/send")
    public ResponseEntity<String> send(@Valid @RequestBody NotificationRequest request,
                                       Authentication authentication) {

        String userEmail = authentication.getName();

        // For now just simulate sending notification
        System.out.println("📩 Notification to " + userEmail + " => " + request.getMessage());

        return ResponseEntity.ok("Notification sent successfully!");
    }
}
