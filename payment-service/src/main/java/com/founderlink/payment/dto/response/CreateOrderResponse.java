package com.founderlink.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private Long investmentId;
}
