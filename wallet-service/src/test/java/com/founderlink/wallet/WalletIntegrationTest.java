package com.founderlink.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.wallet.dto.request.WalletDepositRequestDto;
import com.founderlink.wallet.entity.Wallet;
import com.founderlink.wallet.repository.WalletRepository;
import com.founderlink.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "spring.cloud.config.import-check.enabled=false"})
@AutoConfigureMockMvc
class WalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletRepository walletRepository;

    @MockBean
    private WalletTransactionRepository walletTransactionRepository;

    @Test
    void createWalletE2E() throws Exception {
        Long startupId = 200L;
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.ZERO);

        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        mockMvc.perform(post("/wallets/{startupId}", startupId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Wallet created successfully"))
                .andExpect(jsonPath("$.data.startupId").value(startupId))
                .andExpect(jsonPath("$.data.balance").value(0.0));
    }

    @Test
    void depositFundsE2E() throws Exception {
        Long startupId = 200L;
        WalletDepositRequestDto request = new WalletDepositRequestDto(
                999L, startupId, BigDecimal.valueOf(500), 888L, "idem-key");

        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.valueOf(100));

        when(walletTransactionRepository.findByReferenceId(999L)).thenReturn(Optional.empty());
        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/wallets/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Funds deposited successfully"))
                .andExpect(jsonPath("$.data.balance").value(600.0));
    }

    @Test
    void getWalletE2E() throws Exception {
        Long startupId = 200L;
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setStartupId(startupId);
        wallet.setBalance(BigDecimal.valueOf(1000));

        when(walletRepository.findByStartupId(startupId)).thenReturn(Optional.of(wallet));

        mockMvc.perform(get("/wallets/{startupId}", startupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Wallet retrieved successfully"))
                .andExpect(jsonPath("$.data.balance").value(1000.0));
    }
}
