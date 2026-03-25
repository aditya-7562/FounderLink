# InvestmentPaymentSagaOrchestrator Update

## Issue
The `handleInvestmentRejected` method would throw `PaymentNotFoundException` if an investment was rejected **before** it was approved, since no Payment entity would exist yet.

## Scenario
```
1. POST /investments → Investment created (PENDING)
2. PUT /investments/{id}/reject → Investment rejected
   └─> Publishes InvestmentRejectedEvent
   └─> ❌ handleInvestmentRejected tries to find Payment
   └─> ❌ Throws PaymentNotFoundException (Payment doesn't exist yet)
```

## Fix Applied

### Before:
```java
@RabbitListener(queues = "investment.rejected.queue")
@Transactional
public void handleInvestmentRejected(InvestmentRejectedEvent event) {
    Payment payment = paymentRepository.findByInvestmentId(event.getInvestmentId())
            .orElseThrow(() -> new PaymentNotFoundException(
                    "No payment found for investment " + event.getInvestmentId()));
    
    // Mark as failed...
}
```

**Problem**: Throws exception if Payment doesn't exist

---

### After:
```java
@RabbitListener(queues = "investment.rejected.queue")
@Transactional
public void handleInvestmentRejected(InvestmentRejectedEvent event) {
    Optional<Payment> paymentOpt = paymentRepository.findByInvestmentId(event.getInvestmentId());

    // If no payment exists, investment was rejected before approval - nothing to do
    if (paymentOpt.isEmpty()) {
        log.info("No payment found for rejected investment {} - likely rejected before approval", 
                event.getInvestmentId());
        return;
    }

    Payment payment = paymentOpt.get();
    
    // Mark as failed...
}
```

**Solution**: Gracefully handles missing Payment

---

## Flow Coverage

### Case 1: Rejection Before Approval
```
POST /investments → PENDING
PUT /investments/{id}/reject → REJECTED
  └─> InvestmentRejectedEvent published
  └─> handleInvestmentRejected called
  └─> ✅ No Payment exists → logs and returns (no error)
```

### Case 2: Rejection After Approval
```
POST /investments → PENDING
PUT /investments/{id}/approve → APPROVED
  └─> InvestmentApprovedEvent published
  └─> ✅ Payment created (PENDING)
PUT /investments/{id}/reject → REJECTED
  └─> InvestmentRejectedEvent published
  └─> handleInvestmentRejected called
  └─> ✅ Payment exists → marks as FAILED
```

### Case 3: Rejection After Payment Initiated
```
POST /investments → PENDING
PUT /investments/{id}/approve → APPROVED
  └─> Payment created (PENDING)
POST /payments/create-order → Order created
  └─> Payment updated (INITIATED)
PUT /investments/{id}/reject → REJECTED
  └─> InvestmentRejectedEvent published
  └─> handleInvestmentRejected called
  └─> ✅ Payment exists → marks as FAILED
```

### Case 4: Rejection After Payment Success
```
POST /investments → PENDING
PUT /investments/{id}/approve → APPROVED
  └─> Payment created (PENDING)
POST /payments/create-order → Order created (INITIATED)
POST /payments/confirm → Payment confirmed (SUCCESS)
PUT /investments/{id}/reject → REJECTED (should be blocked by investment-service)
  └─> If event somehow published:
  └─> handleInvestmentRejected called
  └─> ✅ Payment status = SUCCESS → logs warning, no change
```

---

## Summary

**Change**: Made `handleInvestmentRejected` resilient to missing Payment entities

**Reason**: Payment is only created when investment is approved, so rejection before approval would cause exception

**Impact**: 
- ✅ No more PaymentNotFoundException on early rejections
- ✅ Graceful handling of all rejection scenarios
- ✅ Maintains correct behavior for post-approval rejections

**Files Modified**: 
- `InvestmentPaymentSagaOrchestrator.java`

**Status**: ✅ UPDATED
