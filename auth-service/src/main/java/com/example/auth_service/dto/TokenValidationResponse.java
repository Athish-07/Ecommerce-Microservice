package com.example.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenValidationResponse {
    private boolean valid;
    private String email;
    private String role;
}
