package com.founderlink.payment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void toResponseDto_ReturnsNullWhenInputIsNull() {
        assertThat(mapper.toResponseDto(null)).isNull();
    }

    @Test
    void toResponseDto_MapsAllFields() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setRazorpayPaymentId("ext_id");
        
        PaymentResponseDto dto = mapper.toResponseDto(payment);
        
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getExternalPaymentId()).isEqualTo("ext_id");
    }
}
