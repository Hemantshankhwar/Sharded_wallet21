package com.example.sharded_wallet21.controllers;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sharded_wallet21.dtos.CreateWalletRequestDTO;
import com.example.sharded_wallet21.dtos.CreditWalletRequestDTO;
import com.example.sharded_wallet21.dtos.DebitWalletRequestDTO;
import com.example.sharded_wallet21.entities.Wallet;
import com.example.sharded_wallet21.services.IdempotencyService;
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

    @PostMapping
    public ResponseEntity<Wallet> createWallet(@RequestBody CreateWalletRequestDTO request) {
        try {
            Wallet newWallet = walletService.createWallet(request.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(newWallet);
        } catch (Exception e) {
            log.error("Error creating wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Wallet> getWalletById(@PathVariable Long id) {
        Wallet wallet = walletService.getWalletById(id);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getWalletBalance(@PathVariable Long id) {
        BigDecimal balance = walletService.getWalletBalance(id);
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<?> debitWallet(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long id, @RequestBody DebitWalletRequestDTO request) {
        return idempotencyService.execute("debit:" + id, idempotencyKey, request, () -> {
            walletService.debit(id, request.getAmount());
            return ResponseEntity.ok(walletService.getWalletById(id));
        });
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<?> creditWallet(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long id, @RequestBody CreditWalletRequestDTO request) {
        return idempotencyService.execute("credit:" + id, idempotencyKey, request, () -> {
            walletService.credit(id, request.getAmount());
            return ResponseEntity.ok(walletService.getWalletById(id));
        });
    }
}
