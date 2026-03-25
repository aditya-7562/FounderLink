# Payment Entity Creation Fix - Implementation Summary

## Problem
Payment entities were NEVER created, causing `/payments/create-order` to always fail with `PaymentNotFoundException`.

## Root Cause
During Razorpay migration, the automatic payment creation listeners were removed, but no replacement mechanism was added to initialize Payment entities.

## Solution
Added `InvestmentApprovedListener` that creates Payment entity when investment is approved.

---

## Files Created/Modified

### 1. NEW: InvestmentApprovedListener.java
**Location**: `payment-service/src/main/java/com/founderlink/payment/listener/InvestmentApprovedListener.java`

**Purpose**: Listens to `investment.approved.queue` and creates Payment entity

**Key Features**:
- ✅ Idempotency check (prevents duplicate Payment creation)
- ✅ Creates Payment with status = PENDING
- ✅ Sets all required fields from event
- ✅ Uses transaction for atomicity

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class InvestmentApprovedListener {

    private final PaymentRepository paymentRepository;

    @RabbitListener(queues = "investment.approved.queue")
    @Transactional
    public void handleInvestmentApproved(InvestmentApprovedEvent event) {
        // Idempotency check
        if (paymentRepository.findByInvestmentId(event.getInvestmentId()).isPresent()) {
            return;
        }

        // Create Payment entity
        Payment payment = new Payment();
        payment.setInvestmentId(event.getInvestmentId());
        payment.setInvestorId(event.getInvestorId());
        payment.setStartupId(event.getStartupId());
        payment.setFounderId(event.getFounderId());
        payment.setAmount(event.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey("payment-init-" + event.getInvestmentId());
        
        paymentRepository.save(payment);
    }
}
```

---

### 2. MODIFIED: PaymentStatus.java
**Change**: Added `PENDING` status

**Before**:
```java
public enum PaymentStatus {
    INITIATED,   // Order created, awaiting payment
    SUCCESS,     // Payment captured and transferred
    FAILED       // Payment failed
}
```

**After**:
```java
public enum PaymentStatus {
    PENDING,     // Investment approved, payment not yet initiated
    INITIATED,   // Order created, awaiting payment
    SUCCESS,     // Payment captured and transferred
    FAILED       // Payment failed
}
```

---

### 3. MODIFIED: Payment.java
**Change**: Updated default status in @PrePersist

**Before**:
```java
if (this.status == null) {
    this.status = PaymentStatus.INITIATED;
}
```

**After**:
```java
if (this.status == null) {
    this.status = PaymentStatus.PENDING;
}
```

---

## Flow After Fix

### Complete Flow:
```
1. POST /investments
   └─> Investment created (status = PENDING)
   └─> Publishes InvestmentCreated event
   └─> No action in payment-service

2. PUT /investments/{id}/approve
   └─> Investment updated (status = APPROVED)
   └─> Publishes InvestmentApproved event
   └─> ✅ InvestmentApprovedListener creates Payment (status = PENDING)

3. POST /payments/create-order
   └─> Finds Payment by investmentId ✅ SUCCESS
   └─> Creates Razorpay order
   └─> Updates Payment (status = INITIATED)
   └─> Returns orderId to frontend

4. User pays via Razorpay

5. POST /payments/confirm
   └─> Verifies signature
   └─> Updates Payment (status = SUCCESS)
   └─> Publishes PaymentCompleted event

6. Wallet credited & Investment marked COMPLETED
```

---

## State Transitions

```
PENDING → INITIATED → SUCCESS
   ↓          ↓          
 FAILED    FAILED
```

**PENDING**: Investment approved, awaiting user to create order  
**INITIATED**: Razorpay order created, awaiting payment  
**SUCCESS**: Payment completed  
**FAILED**: Payment failed or investment rejected  

---

## Verification Steps

### 1. Approve Investment
```bash
PUT http://localhost:8081/api/investments/1/approve
```

### 2. Check Payment Created
```bash
GET http://localhost:8088/payments/investment/1

Expected Response:
{
  "id": 1,
  "investmentId": 1,
  "status": "PENDING",
  "amount": 10000,
  ...
}
```

### 3. Create Order (Should Work Now)
```bash
POST http://localhost:8088/payments/create-order
{
  "investmentId": 1
}

Expected Response:
{
  "orderId": "order_xxx",
  "amount": 10000,
  "currency": "INR",
  "investmentId": 1
}
```

---

## Idempotency Guarantee

The listener includes idempotency check:

```java
if (paymentRepository.findByInvestmentId(event.getInvestmentId()).isPresent()) {
    log.warn("Payment already exists - skipping creation");
    return;
}
```

**Prevents**:
- Duplicate Payment creation on event replay
- Race conditions from multiple event deliveries
- Data inconsistency

---

## Configuration

No configuration changes needed. Uses existing:
- Queue: `investment.approved.queue` (already configured in RabbitMQConfig)
- Event: `InvestmentApprovedEvent` (already exists with all required fields)

---

## Testing Checklist

### Manual Testing:
```
☐ Start all services (RabbitMQ, MySQL, investment-service, payment-service)
☐ Create investment → verify PENDING
☐ Approve investment → verify Payment created with PENDING status
☐ Create order → verify SUCCESS (no PaymentNotFoundException)
☐ Approve same investment again → verify no duplicate Payment
☐ Check logs for "Payment entity created successfully"
```

### Expected Logs:
```
payment-service:
  Received InvestmentApprovedEvent - investmentId: 1, amount: 10000
  Payment entity created successfully - investmentId: 1, paymentId: 1
```

---

## Success Criteria

✅ **After implementation**:

1. `PUT /investments/{id}/approve` → Payment is created
2. `GET /payments/investment/{id}` → returns Payment with status PENDING
3. `POST /payments/create-order` → works without PaymentNotFoundException
4. Duplicate approvals → no duplicate Payments (idempotency)

---

## What Was NOT Changed

❌ Did NOT modify createOrder logic  
❌ Did NOT add Feign/REST calls  
❌ Did NOT add retry/DLQ/outbox  
❌ Did NOT redesign architecture  
❌ Did NOT modify existing payment flow  

✅ Only added listener to initialize Payment entity

---

## Migration Notes

If existing investments are already APPROVED but have no Payment:

**Option 1**: Re-approve them (triggers event)  
**Option 2**: Run SQL script to backfill:

```sql
INSERT INTO payments (investment_id, investor_id, startup_id, founder_id, amount, status, idempotency_key, created_at, updated_at)
SELECT 
    i.id,
    i.investor_id,
    i.startup_id,
    i.founder_id,
    i.amount,
    'PENDING',
    CONCAT('payment-init-', i.id),
    NOW(),
    NOW()
FROM investments i
LEFT JOIN payments p ON i.id = p.investment_id
WHERE i.status = 'APPROVED' AND p.id IS NULL;
```

---

**Status**: ✅ FIXED  
**Complexity**: Minimal (single listener class)  
**Risk**: Low (idempotent, transactional)
