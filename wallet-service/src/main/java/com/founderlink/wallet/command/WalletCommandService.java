package com.founderlink.wallet.command;

import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;
import com.founderlink.wallet.entity.WalletTransaction;
import com.founderlink.wallet.exception.WalletNotFoundException;
import com.founderlink.wallet.mapper.WalletMapper;
import com.founderlink.wallet.repository.WalletRepository;
import com.founderlink.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WalletCommandService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletMapper walletMapper;

    public WalletResponseDto createWallet(Long startupId) {
        log.info("COMMAND - createWallet: startupId={}", startupId);
        if (walletRepository.findByStartupId(startupId).isPresent()) {
            Wallet existing = walletRepository.findByStartupId(startupId).get();
            return walletMapper.toResponseDto(existing);
        }
        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.ZERO);
        return walletMapper.toResponseDto(walletRepository.save(wallet));
    }

    @Caching(evict = {
        @CacheEvict(value = "walletByStartup", key = "#depositRequest.startupId"),
        @CacheEvict(value = "walletBalance", key = "#depositRequest.startupId")
    })
    public WalletResponseDto depositFunds(WalletDepositRequestDto depositRequest) {
        log.info("COMMAND - depositFunds: startupId={}, referenceId={}",
                depositRequest.getStartupId(), depositRequest.getReferenceId());

        var existingTransaction = walletTransactionRepository.findByReferenceId(depositRequest.getReferenceId());
        if (existingTransaction.isPresent()) {
            return walletMapper.toResponseDto(existingTransaction.get().getWallet());
        }

        Wallet wallet = walletRepository.findByStartupId(depositRequest.getStartupId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for startup ID: " + depositRequest.getStartupId()));

        wallet.setBalance(wallet.getBalance().add(depositRequest.getAmount()));
        wallet.setUpdatedAt(LocalDateTime.now());
        Wallet updatedWallet = walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(updatedWallet);
        transaction.setReferenceId(depositRequest.getReferenceId());
        transaction.setSourcePaymentId(depositRequest.getSourcePaymentId());
        transaction.setIdempotencyKey(depositRequest.getIdempotencyKey());
        transaction.setAmount(depositRequest.getAmount());
        walletTransactionRepository.save(transaction);

        return walletMapper.toResponseDto(updatedWallet);
    }
}
