package com.founderlink.investment.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.founderlink.investment.command.InvestmentCommandService;
import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.exception.InvestmentNotFoundException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.query.InvestmentQueryService;
import com.founderlink.investment.repository.InvestmentRepository;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceImplTest {

    @Mock
    private InvestmentCommandService commandService;

    @Mock
    private InvestmentQueryService queryService;

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InvestmentMapper investmentMapper;

    @InjectMocks
    private InvestmentServiceImpl investmentService;

    private Investment investment;
    private InvestmentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        investment = new Investment();
        investment.setId(1L);
        investment.setStatus(InvestmentStatus.APPROVED);

        responseDto = new InvestmentResponseDto();
        responseDto.setId(1L);
    }

    @Test
    void createInvestment() {
        InvestmentRequestDto requestDto = new InvestmentRequestDto();
        when(commandService.createInvestment(202L, requestDto)).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.createInvestment(202L, requestDto);

        assertThat(result).isNotNull();
        verify(commandService).createInvestment(202L, requestDto);
    }

    @Test
    void getInvestmentsByStartupId_List() {
        when(queryService.getInvestmentsByStartupId(101L, 5L)).thenReturn(List.of(responseDto));

        List<InvestmentResponseDto> result = investmentService.getInvestmentsByStartupId(101L, 5L);

        assertThat(result).hasSize(1);
        verify(queryService).getInvestmentsByStartupId(101L, 5L);
    }

    @Test
    void getInvestmentsByStartupId_Page() {
        Pageable pageable = Pageable.unpaged();
        when(queryService.getInvestmentsByStartupId(101L, 5L, pageable)).thenReturn(new PageImpl<>(List.of(responseDto)));

        Page<InvestmentResponseDto> result = investmentService.getInvestmentsByStartupId(101L, 5L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(queryService).getInvestmentsByStartupId(101L, 5L, pageable);
    }

    @Test
    void getInvestmentsByInvestorId_List() {
        when(queryService.getInvestmentsByInvestorId(202L)).thenReturn(List.of(responseDto));

        List<InvestmentResponseDto> result = investmentService.getInvestmentsByInvestorId(202L);

        assertThat(result).hasSize(1);
        verify(queryService).getInvestmentsByInvestorId(202L);
    }

    @Test
    void getInvestmentsByInvestorId_Page() {
        Pageable pageable = Pageable.unpaged();
        when(queryService.getInvestmentsByInvestorId(202L, pageable)).thenReturn(new PageImpl<>(List.of(responseDto)));

        Page<InvestmentResponseDto> result = investmentService.getInvestmentsByInvestorId(202L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(queryService).getInvestmentsByInvestorId(202L, pageable);
    }

    @Test
    void updateInvestmentStatus() {
        InvestmentStatusUpdateDto updateDto = new InvestmentStatusUpdateDto();
        when(commandService.updateInvestmentStatus(1L, 5L, updateDto)).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.updateInvestmentStatus(1L, 5L, updateDto);

        assertThat(result).isNotNull();
        verify(commandService).updateInvestmentStatus(1L, 5L, updateDto);
    }

    @Test
    void getInvestmentById() {
        when(queryService.getInvestmentById(1L)).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.getInvestmentById(1L);

        assertThat(result).isNotNull();
        verify(queryService).getInvestmentById(1L);
    }

    @Test
    void markCompletedFromPayment_Success() {
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(investmentRepository.save(any(Investment.class))).thenReturn(investment);
        when(investmentMapper.toResponseDto(any(Investment.class))).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.markCompletedFromPayment(1L);

        assertThat(result).isNotNull();
        assertThat(investment.getStatus()).isEqualTo(InvestmentStatus.COMPLETED);
    }

    @Test
    void markCompletedFromPayment_AlreadyCompleted() {
        investment.setStatus(InvestmentStatus.COMPLETED);
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(investmentMapper.toResponseDto(investment)).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.markCompletedFromPayment(1L);

        assertThat(result).isNotNull();
        verify(investmentRepository).findById(1L);
    }

    @Test
    void markCompletedFromPayment_NotApproved() {
        investment.setStatus(InvestmentStatus.PENDING);
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(investmentMapper.toResponseDto(investment)).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.markCompletedFromPayment(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void markCompletedFromPayment_NotFound() {
        when(investmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentService.markCompletedFromPayment(1L))
                .isInstanceOf(InvestmentNotFoundException.class);
    }

    @Test
    void markPaymentFailedFromPayment_Success() {
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(investmentRepository.save(any(Investment.class))).thenReturn(investment);
        when(investmentMapper.toResponseDto(any(Investment.class))).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.markPaymentFailedFromPayment(1L);

        assertThat(result).isNotNull();
        assertThat(investment.getStatus()).isEqualTo(InvestmentStatus.PAYMENT_FAILED);
    }

    @Test
    void markPaymentFailedFromPayment_AlreadyFailed() {
        investment.setStatus(InvestmentStatus.PAYMENT_FAILED);
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(investmentMapper.toResponseDto(investment)).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.markPaymentFailedFromPayment(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void markPaymentFailedFromPayment_NotApproved() {
        investment.setStatus(InvestmentStatus.PENDING);
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(investmentMapper.toResponseDto(investment)).thenReturn(responseDto);

        InvestmentResponseDto result = investmentService.markPaymentFailedFromPayment(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void markPaymentFailedFromPayment_NotFound() {
        when(investmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentService.markPaymentFailedFromPayment(1L))
                .isInstanceOf(InvestmentNotFoundException.class);
    }
}
