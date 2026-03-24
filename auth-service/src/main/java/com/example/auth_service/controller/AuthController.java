package com.example.auth_service.controller;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.dto.TokenValidationResponse;
import com.example.auth_service.security.JwtService;
import com.example.auth_service.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "NOVACART_AUTH";

    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${auth.cookie.secure:false}")
    private boolean secureCookie;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse servletResponse
    ) {
        AuthResponse response = authService.login(request);
        servletResponse.addCookie(buildAuthCookie(response.getToken()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = AUTH_COOKIE_NAME, required = false) String authCookie
    ) {
        String token = resolveToken(authHeader, authCookie);

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenValidationResponse(false, null, null));
        }

        boolean valid = jwtService.isTokenValid(token);

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenValidationResponse(false, null, null));
        }

        return ResponseEntity.ok(
                new TokenValidationResponse(
                        true,
                        jwtService.extractEmail(token),
                        jwtService.extractRole(token)
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletResponse servletResponse) {
        servletResponse.addCookie(clearAuthCookie());
        return ResponseEntity.ok(new AuthResponse("Logout successful!", null));
    }

    private String resolveToken(String authHeader, String authCookie) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (authCookie != null && !authCookie.isBlank()) {
            return authCookie;
        }
        return null;
    }

    private Cookie buildAuthCookie(String token) {
        Cookie cookie = new Cookie(AUTH_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(Math.toIntExact(jwtService.getExpirationMs() / 1000));
        return cookie;
    }

    private Cookie clearAuthCookie() {
        Cookie cookie = new Cookie(AUTH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }
}
