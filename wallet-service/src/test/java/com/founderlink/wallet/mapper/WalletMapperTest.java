package com.founderlink.wallet.mapper;

import com.founderlink.wallet.dto.response.WalletResponseDto;
import com.founderlink.wallet.entity.Wallet;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class WalletMapperTest {

    private final WalletMapper mapper = new WalletMapper();

    @Test
    void toResponseDto_MapsAllFields() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setStartupId(100L);
        wallet.setBalance(BigDecimal.valueOf(500.50));

        WalletResponseDto dto = mapper.toResponseDto(wallet);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStartupId()).isEqualTo(100L);
        assertThat(dto.getBalance()).isEqualByComparingTo("500.50");
    }

    @Test
    void toResponseDto_NullWallet_ReturnsNull() {
        assertThat(mapper.toResponseDto(null)).isNull();
    }
}
