package com.founderlink.wallet.service;

import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.WalletResponseDto;

public interface WalletService {

    /**
     * Create wallet for startup (called once per startup).
     */
    WalletResponseDto createWallet(Long startupId);

    /**
     * Deposit funds to startup wallet.
     * Uses idempotency to prevent double-deposits.
     */
    WalletResponseDto depositFunds(WalletDepositRequestDto depositRequest);

    /**
     * Get wallet by startup ID.
     */
    WalletResponseDto getWalletByStartupId(Long startupId);

    /**
     * Get wallet balance.
     */
    java.math.BigDecimal getBalance(Long startupId);
}
