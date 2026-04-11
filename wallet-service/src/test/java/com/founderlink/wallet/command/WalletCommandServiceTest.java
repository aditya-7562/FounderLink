package com.founderlink.wallet.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;
import com.founderlink.wallet.entity.WalletTransaction;
import com.founderlink.wallet.exception.WalletNotFoundException;
import com.founderlink.wallet.mapper.WalletMapper;
import com.founderlink.wallet.repository.WalletRepository;
import com.founderlink.wallet.repository.WalletTransactionRepository;

@ExtendWith(MockitoExtension.class)
class WalletCommandServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletCommandService commandService;

    @Test
    void createWallet_New_Success() {
        Long startupId = 1L;
        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.ZERO);
        
        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletMapper.toResponseDto(wallet)).thenReturn(new WalletResponseDto());

        commandService.createWallet(startupId);

        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_Existing_ReturnsExisting() {
        Long startupId = 1L;
        Wallet wallet = new Wallet();
        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.of(wallet));
        when(walletMapper.toResponseDto(wallet)).thenReturn(new WalletResponseDto());

        commandService.createWallet(startupId);

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void depositFunds_NewTransaction_Success() {
        WalletDepositRequestDto request = new WalletDepositRequestDto(
                100L, 1L, BigDecimal.valueOf(500), 200L, "idem-123");
        
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(100));
        
        when(walletTransactionRepository.findByReferenceId(100L)).thenReturn(Optional.empty());
        when(walletRepository.findByStartupId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletMapper.toResponseDto(wallet)).thenReturn(new WalletResponseDto());

        commandService.depositFunds(request);

        verify(walletRepository).save(wallet);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
        assertThat(wallet.getBalance()).isEqualByComparingTo("600");
    }

    @Test
    void depositFunds_DuplicateTransaction_ReturnsExisting() {
        WalletDepositRequestDto request = new WalletDepositRequestDto(
                100L, 1L, BigDecimal.valueOf(500), 200L, "idem-123");
        
        WalletTransaction transaction = new WalletTransaction();
        Wallet wallet = new Wallet();
        transaction.setWallet(wallet);

        when(walletTransactionRepository.findByReferenceId(100L)).thenReturn(Optional.of(transaction));
        when(walletMapper.toResponseDto(wallet)).thenReturn(new WalletResponseDto());

        commandService.depositFunds(request);

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void depositFunds_WalletNotFound_ThrowsException() {
        WalletDepositRequestDto request = new WalletDepositRequestDto(
                100L, 1L, BigDecimal.valueOf(500), 200L, "idem-123");
        
        when(walletTransactionRepository.findByReferenceId(100L)).thenReturn(Optional.empty());
        when(walletRepository.findByStartupId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.depositFunds(request))
                .isInstanceOf(WalletNotFoundException.class);
    }
}
