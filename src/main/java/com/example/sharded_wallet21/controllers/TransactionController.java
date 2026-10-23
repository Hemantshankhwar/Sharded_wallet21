package com.example.sharded_wallet21.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.example.sharded_wallet21.config.Authz;
import com.example.sharded_wallet21.dtos.TransferRequestDTO;
import com.example.sharded_wallet21.dtos.TransferResponseDTO;
import com.example.sharded_wallet21.entities.Transaction;
import com.example.sharded_wallet21.services.IdempotencyService;
import com.example.sharded_wallet21.services.TransactionService;
import com.example.sharded_wallet21.services.TransferSagaService;
import com.example.sharded_wallet21.services.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/transactions")
public class TransactionController {

    private final TransferSagaService transferSagaService;
    private final IdempotencyService idempotencyService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final Authz authz;

    // Without walletId this lists every transaction, which only admins may do.
    @GetMapping
    @PreAuthorize("#walletId != null ? @authz.ownsWallet(#walletId) : hasRole('ADMIN')")
    public ResponseEntity<List<Transaction>> getTransactionsByWallet(@RequestParam(required = false) Long walletId) {
        return ResponseEntity.ok(walletId == null
                ? transactionService.getAllTransactions()
                : transactionService.getTransactionByWalletId(walletId));
    }

    @PostMapping
    @PreAuthorize("@authz.ownsWallet(#transferRequestDTO.fromWalletId)")
    public ResponseEntity<?> createTransaction(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferRequestDTO transferRequestDTO) {
        validate(transferRequestDTO);
        Long userId = authz.currentUserId();
        return idempotencyService.execute(userId + ":transfer", idempotencyKey, transferRequestDTO,
                () -> processTransfer(transferRequestDTO, userId));
    }

    private void validate(TransferRequestDTO dto) {
        if (dto.getFromWalletId() == null || dto.getToWalletId() == null) {
            throw new IllegalArgumentException("fromWalletId and toWalletId are required");
        }
        if (dto.getFromWalletId().equals(dto.getToWalletId())) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        walletService.getWalletById(dto.getToWalletId());
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            dto.setDescription("Transfer");
        }
    }

    private ResponseEntity<TransferResponseDTO> processTransfer(TransferRequestDTO transferRequestDTO, Long userId) {
        try {
            Long sagaInstanceId = transferSagaService.initiateTransfer(
                    transferRequestDTO.getFromWalletId(),
                    transferRequestDTO.getToWalletId(),
                    transferRequestDTO.getAmount(),
                    transferRequestDTO.getDescription(),
                    userId);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    TransferResponseDTO.builder()
                            .sagaInstanceId(sagaInstanceId)
                            .build());
        } catch (Exception e) {
            log.error("Error creating transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
