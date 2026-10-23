package com.example.sharded_wallet21.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.sharded_wallet21.dtos.AuthDtos.AdminCreateUserRequest;
import com.example.sharded_wallet21.dtos.AuthDtos.AuthResponse;
import com.example.sharded_wallet21.dtos.AuthDtos.LoginRequest;
import com.example.sharded_wallet21.dtos.AuthDtos.RegisterRequest;
import com.example.sharded_wallet21.entities.Role;
import com.example.sharded_wallet21.entities.User;
import com.example.sharded_wallet21.services.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = authService.createUser(request.name(), request.email(), request.password(), Role.USER);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.toResponse(user));
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> adminCreateUser(@RequestBody AdminCreateUserRequest request) {
        User user = authService.createUser(request.name(), request.email(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
