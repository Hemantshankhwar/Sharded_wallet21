package com.example.sharded_wallet21.controllers;

import com.example.sharded_wallet21.dtos.AuthDtos.RecipientResponse;
import com.example.sharded_wallet21.entities.User;
import com.example.sharded_wallet21.entities.Wallet;
import com.example.sharded_wallet21.services.UserService;
import com.example.sharded_wallet21.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final WalletService walletService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUserById(Long.valueOf(jwt.getSubject())));
    }

    // Exposes only name and wallet ids so users can pick a transfer destination by email.
    @GetMapping("/lookup")
    public ResponseEntity<RecipientResponse> lookup(@RequestParam String email) {
        User user = userService.getUserByEmail(email);
        List<Long> walletIds = walletService.getWalletsByUserId(user.getId()).stream().map(Wallet::getId).toList();
        return ResponseEntity.ok(new RecipientResponse(user.getId(), user.getName(), walletIds));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.isSelfOrAdmin(#id)")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getUsersByName(@RequestParam String name) {
        return ResponseEntity.ok(userService.getUsersByName(name));
    }
}
