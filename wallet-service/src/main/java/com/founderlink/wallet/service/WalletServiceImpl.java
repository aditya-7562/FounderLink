package com.founderlink.wallet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;
import com.founderlink.wallet.exception.WalletNotFoundException;
import com.founderlink.wallet.mapper.WalletMapper;
import com.founderlink.wallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    @Override
    public WalletResponseDto createWallet(Long startupId) {
        log.info("Creating wallet for startup ID: {}", startupId);

        // Check if already exists
        if (walletRepository.findByStartupId(startupId).isPresent()) {
            log.warn("Wallet already exists for startup ID: {}", startupId);
            Wallet existing = walletRepository.findByStartupId(startupId).get();
            return walletMapper.toResponseDto(existing);
        }

        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.ZERO);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("✓ Wallet created - ID: {}, startupId: {}", savedWallet.getId(), startupId);

        return walletMapper.toResponseDto(savedWallet);
    }

    @Override
    public WalletResponseDto depositFunds(WalletDepositRequestDto depositRequest) {
        log.info("Depositing funds to startup {} - amount: ${}, idempotencyKey: {}",
                depositRequest.getStartupId(), depositRequest.getAmount(),
                depositRequest.getIdempotencyKey());

        Wallet wallet = walletRepository.findByStartupId(depositRequest.getStartupId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for startup ID: " + depositRequest.getStartupId()));

        // Idempotency: In production, you would check if this idempotencyKey was already processed
        // For now, we'll just add the amount (in Phase 4, add Redis caching for idempotency)

        wallet.setBalance(wallet.getBalance().add(depositRequest.getAmount()));
        wallet.setUpdatedAt(LocalDateTime.now());

        Wallet updatedWallet = walletRepository.save(wallet);
        log.info("✓ Funds deposited - startup: {}, new balance: ${}, source payment: {}",
                depositRequest.getStartupId(), updatedWallet.getBalance(),
                depositRequest.getSourcePaymentId());

        return walletMapper.toResponseDto(updatedWallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDto getWalletByStartupId(Long startupId) {
        Wallet wallet = walletRepository.findByStartupId(startupId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for startup ID: " + startupId));
        return walletMapper.toResponseDto(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long startupId) {
        return walletRepository.findByStartupId(startupId)
                .map(Wallet::getBalance)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for startup ID: " + startupId));
    }
}
