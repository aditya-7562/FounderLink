package com.founderlink.wallet.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;
import com.founderlink.wallet.exception.WalletNotFoundException;
import com.founderlink.wallet.mapper.WalletMapper;
import com.founderlink.wallet.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletQueryServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletQueryService queryService;

    @Test
    void getWalletByStartupId_Success() {
        Long startupId = 1L;
        Wallet wallet = new Wallet();
        wallet.setStartupId(startupId);
        WalletResponseDto responseDto = new WalletResponseDto();
        
        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.of(wallet));
        when(walletMapper.toResponseDto(wallet)).thenReturn(responseDto);

        WalletResponseDto result = queryService.getWalletByStartupId(startupId);

        assertThat(result).isEqualTo(responseDto);
    }

    @Test
    void getWalletByStartupId_NotFound_ThrowsException() {
        Long startupId = 1L;
        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getWalletByStartupId(startupId))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    @Test
    void getBalance_Success() {
        Long startupId = 1L;
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(100.50));
        
        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.of(wallet));

        BigDecimal balance = queryService.getBalance(startupId);

        assertThat(balance).isEqualByComparingTo("100.50");
    }

    @Test
    void getBalance_NotFound_ThrowsException() {
        Long startupId = 1L;
        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getBalance(startupId))
                .isInstanceOf(WalletNotFoundException.class);
    }
}
