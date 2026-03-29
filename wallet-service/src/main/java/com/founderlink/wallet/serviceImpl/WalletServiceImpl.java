package com.founderlink.wallet.serviceImpl;

import com.founderlink.wallet.command.WalletCommandService;
import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.query.WalletQueryService;
import com.founderlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletCommandService commandService;
    private final WalletQueryService queryService;

    @Override
    public WalletResponseDto createWallet(Long startupId) {
        return commandService.createWallet(startupId);
    }

    @Override
    public WalletResponseDto depositFunds(WalletDepositRequestDto depositRequest) {
        return commandService.depositFunds(depositRequest);
    }

    @Override
    public WalletResponseDto getWalletByStartupId(Long startupId) {
        return queryService.getWalletByStartupId(startupId);
    }

    @Override
    public BigDecimal getBalance(Long startupId) {
        return queryService.getBalance(startupId);
    }
}
