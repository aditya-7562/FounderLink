package com.founderlink.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.founderlink.payment.dto.external.WalletDepositRequestDto;
import com.founderlink.payment.dto.external.WalletResponseDto;

/**
 * Feign client for wallet-service.
 * Communicates with wallet-service to handle fund deposits.
 */
@FeignClient(name = "wallet-service")
public interface WalletServiceClient {

    /**
     * Create wallet for startup.
     * POST /wallets/{startupId}
     */
    @PostMapping("/wallets/{startupId}")
    WalletResponseDto createWallet(@PathVariable Long startupId);

    /**
     * Deposit funds to startup wallet.
     * POST /wallets/deposit
     */
    @PostMapping("/wallets/deposit")
    WalletResponseDto depositFunds(@RequestBody WalletDepositRequestDto depositRequest);

    /**
     * Get wallet details.
     * GET /wallets/{startupId}
     */
    @GetMapping("/wallets/{startupId}")
    WalletResponseDto getWallet(@PathVariable Long startupId);
}
