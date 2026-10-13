package com.example.sharded_wallet21.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDTO {
    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private String description;
}
