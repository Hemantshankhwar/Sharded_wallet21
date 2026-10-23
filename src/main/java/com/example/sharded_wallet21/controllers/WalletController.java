package com.example.sharded_wallet21.controllers;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sharded_wallet21.config.Authz;
import com.example.sharded_wallet21.dtos.CreateWalletRequestDTO;
import com.example.sharded_wallet21.dtos.CreditWalletRequestDTO;
import com.example.sharded_wallet21.dtos.DebitWalletRequestDTO;
import com.example.sharded_wallet21.entities.Wallet;
import com.example.sharded_wallet21.services.IdempotencyService;
import com.example.sharded_wallet21.services.UserService;
import com.example.sharded_wallet21.services.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wallets")
@Slf4j
public class WalletController {
    private final WalletService walletService;
    private final IdempotencyService idempotencyService;
    private final UserService userService;
    private final Authz authz;

    @PostMapping
    public ResponseEntity<Wallet> createWallet(@RequestBody CreateWalletRequestDTO request) {
        Long me = authz.currentUserId();
        Long target = request.getUserId() == null ? me : request.getUserId();
        if (!authz.isAdmin() && !me.equals(target)) {
            throw new AccessDeniedException("Cannot create a wallet for another user");
        }
        userService.getUserById(target);
        try {
            Wallet newWallet = walletService.createWallet(target);
            return ResponseEntity.status(HttpStatus.CREATED).body(newWallet);
        } catch (Exception e) {
            log.error("Error creating wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Wallet>> getMyWallets() {
        return ResponseEntity.ok(walletService.getWalletsByUserId(authz.currentUserId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Wallet>> getWallets(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(userId == null ? walletService.getAllWallets() : walletService.getWalletsByUserId(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.ownsWallet(#id)")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long id) {
        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("@authz.ownsWallet(#id)")
    public ResponseEntity<BigDecimal> getWalletBalance(@PathVariable Long id) {
        BigDecimal balance = walletService.getWalletBalance(id);
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{id}/debit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> debitWallet(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long id, @RequestBody DebitWalletRequestDTO request) {
        return idempotencyService.execute(authz.currentUserId() + ":debit:" + id, idempotencyKey, request, () -> {
            walletService.debit(id, request.getAmount());
            return ResponseEntity.ok(walletService.getWalletById(id));
        });
    }

    @PostMapping("/{id}/credit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> creditWallet(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long id, @RequestBody CreditWalletRequestDTO request) {
        return idempotencyService.execute(authz.currentUserId() + ":credit:" + id, idempotencyKey, request, () -> {
            walletService.credit(id, request.getAmount());
            return ResponseEntity.ok(walletService.getWalletById(id));
        });
    }
}
