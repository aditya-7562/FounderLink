package com.founderlink.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentResponse {
    
    private String status;
    private Long investmentId;
}
