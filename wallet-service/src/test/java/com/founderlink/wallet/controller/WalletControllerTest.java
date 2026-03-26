package com.founderlink.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createWallet() throws Exception {
        Long startupId = 1L;
        WalletResponseDto responseDto = new WalletResponseDto(1L, startupId, BigDecimal.ZERO, null, null);

        when(walletService.createWallet(startupId)).thenReturn(responseDto);

        mockMvc.perform(post("/wallets/{startupId}", startupId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Wallet created successfully"))
                .andExpect(jsonPath("$.data.startupId").value(startupId));
    }

    @Test
    void depositFunds() throws Exception {
        WalletDepositRequestDto request = new WalletDepositRequestDto(
                123L, 1L, BigDecimal.valueOf(100), 456L, "idem-key");
        WalletResponseDto responseDto = new WalletResponseDto(1L, 1L, BigDecimal.valueOf(100), null, null);

        when(walletService.depositFunds(any(WalletDepositRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/wallets/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Funds deposited successfully"))
                .andExpect(jsonPath("$.data.balance").value(100));
    }

    @Test
    void getWallet() throws Exception {
        Long startupId = 1L;
        WalletResponseDto responseDto = new WalletResponseDto(1L, startupId, BigDecimal.valueOf(500), null, null);

        when(walletService.getWalletByStartupId(startupId)).thenReturn(responseDto);

        mockMvc.perform(get("/wallets/{startupId}", startupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Wallet retrieved successfully"))
                .andExpect(jsonPath("$.data.startupId").value(startupId))
                .andExpect(jsonPath("$.data.balance").value(500));
    }
}
