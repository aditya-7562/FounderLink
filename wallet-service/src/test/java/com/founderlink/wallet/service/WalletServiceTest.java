package com.founderlink.wallet.service;

import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;
import com.founderlink.wallet.entity.WalletTransaction;
import com.founderlink.wallet.exception.WalletNotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void createWallet_Success() {
        Long startupId = 1L;
        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.ZERO);

        WalletResponseDto responseDto = new WalletResponseDto(1L, startupId, BigDecimal.ZERO, null, null);

        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(walletMapper.toResponseDto(any(Wallet.class))).thenReturn(responseDto);

        WalletResponseDto result = walletService.createWallet(startupId);

        assertNotNull(result);
        assertEquals(startupId, result.getStartupId());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void createWallet_AlreadyExists() {
        Long startupId = 1L;
        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);

        WalletResponseDto responseDto = new WalletResponseDto(1L, startupId, BigDecimal.ZERO, null, null);

        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.of(wallet));
        when(walletMapper.toResponseDto(any(Wallet.class))).thenReturn(responseDto);

        WalletResponseDto result = walletService.createWallet(startupId);

        assertNotNull(result);
        assertEquals(startupId, result.getStartupId());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void depositFunds_WalletNotFound() {
        WalletDepositRequestDto request = new WalletDepositRequestDto(
                123L, 1L, BigDecimal.valueOf(100), 456L, "idem-key");

        when(walletTransactionRepository.findByReferenceId(123L)).thenReturn(Optional.empty());
        when(walletRepository.findByStartupId(1L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.depositFunds(request));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getWalletByStartupId_Success() {
        Long startupId = 1L;
        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);

        WalletResponseDto responseDto = new WalletResponseDto(1L, startupId, BigDecimal.ZERO, null, null);

        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.of(wallet));
        when(walletMapper.toResponseDto(any(Wallet.class))).thenReturn(responseDto);

        WalletResponseDto result = walletService.getWalletByStartupId(startupId);

        assertNotNull(result);
        assertEquals(startupId, result.getStartupId());
    }

    @Test
    void getWalletByStartupId_NotFound() {
        when(walletRepository.findByStartupId(1L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletByStartupId(1L));
    }

    @Test
    void getBalance_Success() {
        Long startupId = 1L;
        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.valueOf(500));

        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.of(wallet));

        BigDecimal balance = walletService.getBalance(startupId);

        assertEquals(BigDecimal.valueOf(500), balance);
    }
}
