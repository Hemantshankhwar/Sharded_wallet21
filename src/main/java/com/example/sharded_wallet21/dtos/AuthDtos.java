package com.example.sharded_wallet21.dtos;

import java.util.List;

import com.example.sharded_wallet21.entities.Role;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(String email, String password) {
    }

    public record RegisterRequest(String name, String email, String password) {
    }

    public record AdminCreateUserRequest(String name, String email, String password, Role role) {
    }

    public record AuthResponse(String token, Long userId, String name, Role role) {
    }

    public record RecipientResponse(Long userId, String name, List<Long> walletIds) {
    }
}
