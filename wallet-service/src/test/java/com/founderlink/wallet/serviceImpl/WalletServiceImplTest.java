package com.founderlink.wallet.serviceImpl;

import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.wallet.command.WalletCommandService;
import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.query.WalletQueryService;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletCommandService commandService;

    @Mock
    private WalletQueryService queryService;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void createWallet_Delegates() {
        walletService.createWallet(1L);
        verify(commandService).createWallet(1L);
    }

    @Test
    void depositFunds_Delegates() {
        WalletDepositRequestDto request = new WalletDepositRequestDto();
        walletService.depositFunds(request);
        verify(commandService).depositFunds(request);
    }

    @Test
    void getWalletByStartupId_Delegates() {
        walletService.getWalletByStartupId(1L);
        verify(queryService).getWalletByStartupId(1L);
    }

    @Test
    void getBalance_Delegates() {
        walletService.getBalance(1L);
        verify(queryService).getBalance(1L);
    }
}
