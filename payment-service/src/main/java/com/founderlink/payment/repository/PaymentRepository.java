package com.founderlink.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByInvestmentId(Long investmentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByInvestorId(Long investorId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStartupId(Long startupId);
}
