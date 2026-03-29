package com.founderlink.wallet.query;

import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;
import com.founderlink.wallet.exception.WalletNotFoundException;
import com.founderlink.wallet.mapper.WalletMapper;
import com.founderlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WalletQueryService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    @Cacheable(value = "walletByStartup", key = "#startupId")
    public WalletResponseDto getWalletByStartupId(Long startupId) {
        log.info("QUERY - getWalletByStartupId: {} (cache miss, hitting DB)", startupId);
        Wallet wallet = walletRepository.findByStartupId(startupId)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for startup ID: " + startupId));
        return walletMapper.toResponseDto(wallet);
    }

    @Cacheable(value = "walletBalance", key = "#startupId")
    public BigDecimal getBalance(Long startupId) {
        log.info("QUERY - getBalance: {} (cache miss, hitting DB)", startupId);
        return walletRepository.findByStartupId(startupId)
                .map(Wallet::getBalance)
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for startup ID: " + startupId));
    }
}
