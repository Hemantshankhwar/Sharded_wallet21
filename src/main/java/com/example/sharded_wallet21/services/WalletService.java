package com.example.sharded_wallet21.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sharded_wallet21.entities.Wallet;
import com.example.sharded_wallet21.repositories.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    public Wallet createWallet(Long userId) {
        log.info("Creating wallet for user {}", userId);
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .isActive(true)
                .balance(BigDecimal.ZERO)
                .build();
        wallet = walletRepository.save(wallet);
        log.info("Wallet created with id {}", wallet.getId());
        return wallet;
    }

    public Wallet getWalletById(Long id) {
        return walletRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Wallet not found"));
    }

    public List<Wallet> getWalletsByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    @Transactional
    public void debit(Long walletId, BigDecimal amount) {
        log.info("Debiting {} from wallet {}", amount, walletId);
        Wallet wallet = getWalletById(walletId);
        wallet.debit(amount);
        walletRepository.save(wallet);
        log.info("Debit successful for wallet {}", walletId);
    }

    @Transactional
    public void credit(Long walletId, BigDecimal amount) {
        log.info("Crediting {} to wallet {}", amount, walletId);
        Wallet wallet = getWalletById(walletId);
        wallet.credit(amount);
        walletRepository.save(wallet);
        log.info("Credit successful for wallet {}", walletId);
    }

    public BigDecimal getWalletBalance(Long walletId) {
        return getWalletById(walletId).getBalance();
    }
}
