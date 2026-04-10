package com.founderlink.investment.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
public interface InvestmentService {

  
    InvestmentResponseDto createInvestment(Long investorId,
                                           InvestmentRequestDto requestDto);

    List<InvestmentResponseDto> getInvestmentsByStartupId(Long startupId,Long founderId);

    Page<InvestmentResponseDto> getInvestmentsByStartupId(Long startupId, Long founderId, Pageable pageable);


    List<InvestmentResponseDto> getInvestmentsByInvestorId(Long investorId);

    Page<InvestmentResponseDto> getInvestmentsByInvestorId(Long investorId, Pageable pageable);

    InvestmentResponseDto updateInvestmentStatus(Long investmentId,Long founderId,
                                                  InvestmentStatusUpdateDto statusUpdateDto);
    
    InvestmentResponseDto getInvestmentById(Long investmentId);

    InvestmentResponseDto markCompletedFromPayment(Long investmentId);

    InvestmentResponseDto markPaymentFailedFromPayment(Long investmentId);
}
