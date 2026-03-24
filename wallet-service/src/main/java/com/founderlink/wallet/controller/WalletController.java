package com.founderlink.wallet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.ApiResponse;
import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * POST /wallets/{startupId}
     * Create wallet for startup.
     */
    @PostMapping("/{startupId}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> createWallet(
            @PathVariable Long startupId) {

        log.info("POST /wallets/{} - create wallet", startupId);

        WalletResponseDto response = walletService.createWallet(startupId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "Wallet created successfully",
                        response));
    }

    /**
     * POST /wallets/deposit
     * Deposit funds to startup wallet.
     */
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<WalletResponseDto>> depositFunds(
            @Valid @RequestBody WalletDepositRequestDto depositRequest) {

        log.info("POST /wallets/deposit - startupId: {}, amount: ${}",
                depositRequest.getStartupId(), depositRequest.getAmount());

        WalletResponseDto response = walletService.depositFunds(depositRequest);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Funds deposited successfully",
                        response));
    }

    /**
     * GET /wallets/{startupId}
     * Get wallet details.
     */
    @GetMapping("/{startupId}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> getWallet(
            @PathVariable Long startupId) {

        log.info("GET /wallets/{}", startupId);

        WalletResponseDto response = walletService.getWalletByStartupId(startupId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Wallet retrieved successfully",
                        response));
    }
}
