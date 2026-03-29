package com.founderlink.wallet.service;

import com.founderlink.wallet.command.WalletCommandService;
import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;
import com.founderlink.wallet.entity.WalletTransaction;
import com.founderlink.wallet.mapper.WalletMapper;
import com.founderlink.wallet.repository.WalletRepository;
import com.founderlink.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletIdempotencyTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletCommandService walletCommandService;

    @Test
    void duplicateDepositCreditsOnlyOnceByReferenceId() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setStartupId(500L);
        wallet.setBalance(new BigDecimal("100.00"));

        WalletDepositRequestDto request = new WalletDepositRequestDto(
                777L, 500L, new BigDecimal("25.00"), 999L, "wallet-deposit-777");

        WalletTransaction existingTx = new WalletTransaction();
        existingTx.setReferenceId(777L);
        existingTx.setWallet(wallet);

        when(walletTransactionRepository.findByReferenceId(777L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTx));
        when(walletRepository.findByStartupId(500L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletMapper.toResponseDto(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet saved = invocation.getArgument(0);
            return new WalletResponseDto(saved.getId(), saved.getStartupId(), saved.getBalance(), null, null);
        });

        WalletResponseDto first = walletCommandService.depositFunds(request);
        existingTx.setWallet(wallet);
        WalletResponseDto second = walletCommandService.depositFunds(request);

        assertEquals(new BigDecimal("125.00"), first.getBalance());
        assertEquals(new BigDecimal("125.00"), second.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(walletTransactionRepository, times(1)).save(any(WalletTransaction.class));
    }
}
