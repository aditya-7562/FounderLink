package com.founderlink.wallet.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletMapper walletMapper;

    @Override
    public WalletResponseDto createWallet(Long startupId) {
        log.info("Creating wallet for startup ID: {}", startupId);

        if (walletRepository.findByStartupId(startupId).isPresent()) {
            Wallet existing = walletRepository.findByStartupId(startupId).get();
            return walletMapper.toResponseDto(existing);
        }

        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.ZERO);

        Wallet savedWallet = walletRepository.save(wallet);
        return walletMapper.toResponseDto(savedWallet);
    }

    @Override
    public WalletResponseDto depositFunds(WalletDepositRequestDto depositRequest) {
        log.info("Depositing funds for startupId={} referenceId={}",
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
