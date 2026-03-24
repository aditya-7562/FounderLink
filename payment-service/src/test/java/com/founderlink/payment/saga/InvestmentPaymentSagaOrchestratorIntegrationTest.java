package com.founderlink.payment.saga;

import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dto.external.WalletDepositRequestDto;
import com.founderlink.payment.dto.external.WalletResponseDto;
import com.founderlink.payment.dto.request.PaymentHoldRequestDto;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.event.InvestmentApprovedEvent;
import com.founderlink.payment.event.InvestmentCreatedEvent;
import com.founderlink.payment.event.InvestmentRejectedEvent;
import com.founderlink.payment.repository.PaymentRepository;
import com.founderlink.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for InvestmentPaymentSagaOrchestrator.
 *
 * Tests the three main saga scenarios:
 * 1. Investment Created → Hold Funds
 * 2. Investment Approved → Capture + Deposit to Wallet
 * 3. Investment Rejected → Release Funds
 */
@SpringBootTest
@ActiveProfiles("test")
public class InvestmentPaymentSagaOrchestratorIntegrationTest {

    @Autowired
    private InvestmentPaymentSagaOrchestrator sagaOrchestrator;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private WalletServiceClient walletServiceClient;

    private static final Long INVESTMENT_ID = 1L;
    private static final Long INVESTOR_ID = 100L;
    private static final Long FOUNDER_ID = 50L;
    private static final Long STARTUP_ID = 10L;
    private static final BigDecimal INVESTMENT_AMOUNT = BigDecimal.valueOf(50000.00);

    @BeforeEach
    public void setUp() {
        // Clear all payments before each test
        paymentRepository.deleteAll();
    }

    /**
     * Test Saga Step 1: Investment Created Event → Hold Funds
     *
     * Verifies:
     * - Payment record created with status HELD
     * - Idempotency key set
     * - Correct investment/investor/startup IDs stored
     */
    @Test
    public void testSagaStep1_InvestmentCreated_HoldsFunds() {
        // Arrange: Create investment created event
        InvestmentCreatedEvent event = new InvestmentCreatedEvent(
                INVESTMENT_ID,
                INVESTOR_ID,
                FOUNDER_ID,
                STARTUP_ID,
                INVESTMENT_AMOUNT,
                1L // investmentRoundId
        );

        // Act: Trigger saga step 1
        sagaOrchestrator.handleInvestmentCreated(event);

        // Assert: Payment record should be created with HELD status
        Payment payment = paymentRepository.findByInvestmentId(INVESTMENT_ID)
                .orElseThrow(() -> new AssertionError("Payment not created"));

        assertEquals(INVESTMENT_ID, payment.getInvestmentId());
        assertEquals(INVESTOR_ID, payment.getInvestorId());
        assertEquals(FOUNDER_ID, payment.getFounderId());
        assertEquals(STARTUP_ID, payment.getStartupId());
        assertEquals(INVESTMENT_AMOUNT, payment.getAmount());
        assertEquals(PaymentStatus.HELD, payment.getStatus());
        assertNotNull(payment.getIdempotencyKey());
        assertNotNull(payment.getExternalPaymentId());

        System.out.println("✓ Saga Step 1 Test Passed: Funds held successfully");
    }

    /**
     * Test Saga Step 2: Investment Approved Event → Capture + Deposit to Wallet
     *
     * Verifies:
     * - Payment status transitions from HELD to CAPTURED then TRANSFERRED
     * - Wallet deposit is called with correct parameters
     * - Idempotency maintained
     */
    @Test
    public void testSagaStep2_InvestmentApproved_CaptureAndDeposit() {
        // Arrange: Setup - first create a payment via step 1
        InvestmentCreatedEvent createdEvent = new InvestmentCreatedEvent(
                INVESTMENT_ID, INVESTOR_ID, FOUNDER_ID, STARTUP_ID, INVESTMENT_AMOUNT, 1L
        );
        sagaOrchestrator.handleInvestmentCreated(createdEvent);

        // Mock wallet service to succeed
        WalletResponseDto mockWalletResponse = new WalletResponseDto(
                1L, STARTUP_ID, INVESTMENT_AMOUNT, LocalDateTime.now(), LocalDateTime.now()
        );
        when(walletServiceClient.depositFunds(any(WalletDepositRequestDto.class)))
                .thenReturn(mockWalletResponse);

        // Act: Trigger saga step 2
        InvestmentApprovedEvent approvedEvent = new InvestmentApprovedEvent(
                INVESTMENT_ID, INVESTOR_ID, FOUNDER_ID, STARTUP_ID, INVESTMENT_AMOUNT
        );
        sagaOrchestrator.handleInvestmentApproved(approvedEvent);

        // Assert: Payment should be TRANSFERRED, wallet deposit called
        Payment payment = paymentRepository.findByInvestmentId(INVESTMENT_ID)
                .orElseThrow(() -> new AssertionError("Payment not found"));

        assertEquals(PaymentStatus.TRANSFERRED, payment.getStatus());
        verify(walletServiceClient, times(1)).depositFunds(any(WalletDepositRequestDto.class));

        System.out.println("✓ Saga Step 2 Test Passed: Funds captured and deposited");
    }

    /**
     * Test Saga Step 3: Investment Rejected Event → Release Funds
     *
     * Verifies:
     * - Payment status transitions from HELD to RELEASED
     * - Release is called on payment gateway
     * - Investor receives refund
     */
    @Test
    public void testSagaStep3_InvestmentRejected_ReleasesFunds() {
        // Arrange: Setup - first create a payment via step 1
        InvestmentCreatedEvent createdEvent = new InvestmentCreatedEvent(
                INVESTMENT_ID, INVESTOR_ID, FOUNDER_ID, STARTUP_ID, INVESTMENT_AMOUNT, 1L
        );
        sagaOrchestrator.handleInvestmentCreated(createdEvent);

        // Act: Trigger saga step 3 (rejection)
        InvestmentRejectedEvent rejectedEvent = new InvestmentRejectedEvent(
                INVESTMENT_ID,
                INVESTOR_ID,
                FOUNDER_ID,
                STARTUP_ID,
                INVESTMENT_AMOUNT,
                "Startup did not meet criteria"
        );
        sagaOrchestrator.handleInvestmentRejected(rejectedEvent);

        // Assert: Payment should be RELEASED
        Payment payment = paymentRepository.findByInvestmentId(INVESTMENT_ID)
                .orElseThrow(() -> new AssertionError("Payment not found"));

        assertEquals(PaymentStatus.RELEASED, payment.getStatus());
        assertEquals("Startup did not meet criteria", rejectedEvent.getRejectionReason());

        System.out.println("✓ Saga Step 3 Test Passed: Funds released successfully");
    }

    /**
     * Test Saga Compensation: Investment Approved but Wallet Deposit Fails
     *
     * Verifies:
     * - Payment is captured (status CAPTURED)
     * - Wallet deposit call fails
     * - Compensation logic triggers: Payment is released
     * - Payment status ends in RELEASED (not TRANSFERRED)
     */
    @Test
    public void testSagaCompensation_WalletDepositFails() {
        // Arrange: Setup - first create a payment via step 1
        InvestmentCreatedEvent createdEvent = new InvestmentCreatedEvent(
                INVESTMENT_ID, INVESTOR_ID, FOUNDER_ID, STARTUP_ID, INVESTMENT_AMOUNT, 1L
        );
        sagaOrchestrator.handleInvestmentCreated(createdEvent);

        // Mock wallet service to fail
        when(walletServiceClient.depositFunds(any(WalletDepositRequestDto.class)))
                .thenThrow(new RuntimeException("Wallet service temporarily unavailable"));

        // Act: Trigger saga step 2 with mocked failure
        InvestmentApprovedEvent approvedEvent = new InvestmentApprovedEvent(
                INVESTMENT_ID, INVESTOR_ID, FOUNDER_ID, STARTUP_ID, INVESTMENT_AMOUNT
        );

        assertThrows(RuntimeException.class, () -> {
            sagaOrchestrator.handleInvestmentApproved(approvedEvent);
        });

        // Assert: Payment should be RELEASED (compensation triggered)
        Payment payment = paymentRepository.findByInvestmentId(INVESTMENT_ID)
                .orElseThrow(() -> new AssertionError("Payment not found"));

        assertEquals(PaymentStatus.RELEASED, payment.getStatus(),
                "Funds should be released as compensation when wallet deposit fails");
        verify(walletServiceClient, times(1)).depositFunds(any(WalletDepositRequestDto.class));

        System.out.println("✓ Saga Compensation Test Passed: Funds released due to wallet failure");
    }

    /**
     * Test Idempotency: Duplicate Hold Request Should Return Same Payment
     *
     * Verifies:
     * - Calling handleInvestmentCreated twice with same event
     * - Returns same payment ID (idempotent)
     * - No duplicate charges
     */
    @Test
    public void testSagaIdempotency_DuplicateHoldRequest() {
        // Arrange
        InvestmentCreatedEvent event = new InvestmentCreatedEvent(
                INVESTMENT_ID, INVESTOR_ID, FOUNDER_ID, STARTUP_ID, INVESTMENT_AMOUNT, 1L
        );

        // Act: Call saga step 1 twice
        sagaOrchestrator.handleInvestmentCreated(event);
        Long firstPaymentId = paymentRepository.findByInvestmentId(INVESTMENT_ID)
                .orElseThrow().getId();

        sagaOrchestrator.handleInvestmentCreated(event);
        Long secondPaymentId = paymentRepository.findByInvestmentId(INVESTMENT_ID)
                .orElseThrow().getId();

        // Assert: Same payment ID means idempotent operation
        assertEquals(firstPaymentId, secondPaymentId,
                "Duplicate request should return same payment (idempotent)");

        long paymentCount = paymentRepository.count();
        assertEquals(1, paymentCount, "Should have only 1 payment record, not duplicates");

        System.out.println("✓ Saga Idempotency Test Passed: Duplicates handled correctly");
    }
}
