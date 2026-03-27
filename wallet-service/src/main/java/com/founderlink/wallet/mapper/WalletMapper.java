package com.founderlink.wallet.mapper;

import org.springframework.stereotype.Component;

import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;

@Component
public class WalletMapper {

    public WalletResponseDto toResponseDto(Wallet wallet) {
        if (wallet == null) {
            return null;
        }

        WalletResponseDto dto = new WalletResponseDto();
        dto.setId(wallet.getId());
        dto.setStartupId(wallet.getStartupId());
        dto.setBalance(wallet.getBalance());
        dto.setCreatedAt(wallet.getCreatedAt());
        dto.setUpdatedAt(wallet.getUpdatedAt());

        return dto;
    }
}
