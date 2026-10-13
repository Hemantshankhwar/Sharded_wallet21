package com.example.sharded_wallet21.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sharded_wallet21.dtos.TransferRequestDTO;
import com.example.sharded_wallet21.dtos.TransferResponseDTO;
import com.example.sharded_wallet21.services.IdempotencyService;
import com.example.sharded_wallet21.services.TransferSagaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/transactions")
public class TransactionController {

    private final TransferSagaService transferSagaService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<?> createTransaction(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferRequestDTO transferRequestDTO) {
        return idempotencyService.execute("transfer", idempotencyKey, transferRequestDTO,
                () -> processTransfer(transferRequestDTO));
    }

    private ResponseEntity<TransferResponseDTO> processTransfer(TransferRequestDTO transferRequestDTO) {
        try {
            Long sagaInstanceId = transferSagaService.initiateTransfer(
                    transferRequestDTO.getFromWalletId(),
                    transferRequestDTO.getToWalletId(),
                    transferRequestDTO.getAmount(),
                    transferRequestDTO.getDescription());

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
