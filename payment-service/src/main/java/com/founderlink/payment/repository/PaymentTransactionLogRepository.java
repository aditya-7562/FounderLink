package com.founderlink.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.founderlink.payment.entity.PaymentTransactionLog;

@Repository
public interface PaymentTransactionLogRepository extends JpaRepository<PaymentTransactionLog, Long> {

    List<PaymentTransactionLog> findByPaymentId(Long paymentId);
}
