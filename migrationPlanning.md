# Payment System Audit & Razorpay Migration Plan

## STEP 1 — AUDIT CURRENT IMPLEMENTATION

### Current Flow (As Implemented)

```
1. POST /investments
   └─> investment-service creates Investment (status = PENDING)
   └─> publishes InvestmentCreated event to RabbitMQ
   
2. payment-service consumes InvestmentCreated
   └─> calls holdFunds() [Stripe-style authorization]
   └─> creates Payment record (status = PENDING/HELD)
   └─> stores investmentId reference
   
3. PUT /investments/{id}/approve
   └─> investment-service updates status = APPROVED
   └─> publishes InvestmentApproved event to RabbitMQ
   
4. payment-service consumes InvestmentApproved
   └─> calls captureFunds() [Stripe-style capture]
   └─> updates Payment (status = CAPTURED/TRANSFERRED)
   └─> publishes PaymentCompleted event to RabbitMQ
   
5. wallet-service consumes PaymentCompleted
   └─> credits wallet using investmentId as referenceId (idempotency)
   └─> updates balance
   
6. investment-service consumes PaymentCompleted
   └─> updates Investment (status = COMPLETED)
```

### Tight Coupling Points

| Trigger | Auto-Action | Location |
|---------|-------------|----------|
| Investment creation | `holdFunds()` called automatically | payment-service event consumer |
| Investment approval | `captureFunds()` called automatically | payment-service event consumer |
| Payment capture | Wallet credit triggered | wallet-service event consumer |
| Payment completion | Investment marked COMPLETED | investment-service event consumer |

### Current Event Flow

```
InvestmentCreated → payment-service → holdFunds()
InvestmentApproved → payment-service → captureFunds()
PaymentCompleted → wallet-service → credit()
PaymentCompleted → investment-service → markCompleted()
```

---

## STEP 2 — IDENTIFY WHAT MUST BE REMOVED

### Methods to Remove/Disable

```java
// payment-service
❌ holdFunds() - no longer needed (no authorization hold)
❌ captureFunds() - no longer needed (direct payment)
❌ InvestmentCreatedConsumer.onInvestmentCreated() - should NOT trigger payment
❌ InvestmentApprovedConsumer.onInvestmentApproved() - should NOT trigger capture
```

### Events That Become Invalid/Changed

```
❌ Remove: Automatic payment trigger on InvestmentCreated
❌ Remove: Automatic capture trigger on InvestmentApproved
✅ Keep: PaymentCompleted (still needed for wallet/investment updates)
✅ Add: PaymentInitiated (optional, for tracking)
```

### Saga Steps No Longer Needed

```
❌ Hold funds on investment creation
❌ Capture funds on approval
✅ Keep: Credit wallet on payment success
✅ Keep: Mark investment completed on payment success
```

---

## STEP 3 — DESIGN RAZORPAY INTEGRATION (MINIMAL)

### Backend Responsibilities

#### 1. Create Razorpay Order
```java
// payment-service
POST /api/payments/create-order

Request:
{
  "investmentId": "inv_123",
  "amount": 10000
}

Action:
1. Verify investment exists and status = APPROVED
2. Call Razorpay API: create order
3. Store:
   - investmentId
   - razorpayOrderId
   - amount
   - status = INITIATED
4. Return order details to frontend

Response:
{
  "orderId": "order_razorpay_xyz",
  "amount": 10000,
  "currency": "INR",
  "investmentId": "inv_123"
}
```

#### 2. Handle Payment Success
```java
// payment-service
POST /api/payments/confirm

Request:
{
  "razorpayOrderId": "order_razorpay_xyz",
  "razorpayPaymentId": "pay_razorpay_abc",
  "razorpaySignature": "signature_hash"
}

Action:
1. Verify signature (basic validation)
2. Update Payment:
   - razorpayPaymentId
   - status = SUCCESS
3. Publish PaymentCompleted event
4. Return success

Response:
{
  "status": "SUCCESS",
  "investmentId": "inv_123"
}
```

#### 3. Webhook Handler (Optional but Recommended)
```java
// payment-service
POST /api/payments/webhook

Action:
1. Verify webhook signature (basic)
2. If payment.captured:
   - Update Payment status = SUCCESS
   - Publish PaymentCompleted event
3. Handle idempotency (check if already processed)
```

### What NOT to Implement

```
❌ Advanced retry mechanisms
❌ Complex webhook signature verification (use Razorpay SDK basic method)
❌ Payment refund flow
❌ Partial payments
❌ Payment status polling
❌ Advanced fraud detection
```

---

## STEP 4 — NEW FLOW DESIGN

### Exact New Flow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. CREATE INVESTMENT                                        │
└─────────────────────────────────────────────────────────────┘
POST /api/investments
{
  "userId": "user123",
  "startupId": "startup456",
  "amount": 10000
}
→ investment-service: status = PENDING
→ NO payment action triggered


┌─────────────────────────────────────────────────────────────┐
│ 2. FOUNDER APPROVES                                         │
└─────────────────────────────────────────────────────────────┘
PUT /api/investments/{id}/approve
→ investment-service: status = APPROVED
→ publishes InvestmentApproved event (for notification only)
→ NO payment action triggered


┌─────────────────────────────────────────────────────────────┐
│ 3. INVESTOR INITIATES PAYMENT (USER ACTION)                 │
└─────────────────────────────────────────────────────────────┘
POST /api/payments/create-order
{
  "investmentId": "inv_123"
}
→ payment-service:
  - validates investment status = APPROVED
  - calls Razorpay: create order
  - stores Payment record (status = INITIATED)
  - returns razorpayOrderId

Response:
{
  "orderId": "order_razorpay_xyz",
  "amount": 10000,
  "currency": "INR"
}


┌─────────────────────────────────────────────────────────────┐
│ 4. FRONTEND OPENS RAZORPAY CHECKOUT                         │
└─────────────────────────────────────────────────────────────┘
Frontend:
- Receives orderId
- Opens Razorpay checkout modal
- User completes payment


┌─────────────────────────────────────────────────────────────┐
│ 5. PAYMENT SUCCESS CALLBACK                                 │
└─────────────────────────────────────────────────────────────┘
POST /api/payments/confirm
{
  "razorpayOrderId": "order_razorpay_xyz",
  "razorpayPaymentId": "pay_razorpay_abc",
  "razorpaySignature": "signature"
}
→ payment-service:
  - verifies signature
  - updates Payment (status = SUCCESS)
  - publishes PaymentCompleted event to RabbitMQ


┌─────────────────────────────────────────────────────────────┐
│ 6. WALLET CREDIT (EVENT-DRIVEN)                             │
└─────────────────────────────────────────────────────────────┘
wallet-service consumes PaymentCompleted
→ credits wallet (idempotent via investmentId)


┌─────────────────────────────────────────────────────────────┐
│ 7. INVESTMENT COMPLETION (EVENT-DRIVEN)                     │
└─────────────────────────────────────────────────────────────┘
investment-service consumes PaymentCompleted
→ updates Investment (status = COMPLETED)
```

---

## STEP 5 — DATA MODEL CHANGES

### Payment Entity (payment-service)

```java
// BEFORE
@Entity
public class Payment {
    private String id;
    private String investmentId;
    private BigDecimal amount;
    private PaymentStatus status; // PENDING, HELD, CAPTURED, TRANSFERRED
    private String transactionId; // mock/Stripe ID
}

// AFTER
@Entity
public class Payment {
    private String id;
    private String investmentId;
    private BigDecimal amount;
    private PaymentStatus status; // INITIATED, SUCCESS, FAILED
    
    // NEW FIELDS
    private String razorpayOrderId;    // order_xxx
    private String razorpayPaymentId;  // pay_xxx (null until payment)
    private String razorpaySignature;  // for verification
    
    // REMOVE
    // private String transactionId; (replaced by razorpay IDs)
}
```

### PaymentStatus Enum Changes

```java
// BEFORE
enum PaymentStatus {
    PENDING,
    HELD,
    CAPTURED,
    TRANSFERRED,
    FAILED
}

// AFTER
enum PaymentStatus {
    INITIATED,  // order created, awaiting payment
    SUCCESS,    // payment captured by Razorpay
    FAILED      // payment failed
}
```

### No Changes Required

```
✅ Investment entity (investment-service) - unchanged
✅ Wallet entity (wallet-service) - unchanged
✅ PaymentCompleted event structure - unchanged
```

---

## STEP 6 — VERIFY COMPATIBILITY WITH EXISTING SYSTEM

### RabbitMQ Usage

| Component | Current | After Migration | Status |
|-----------|---------|-----------------|--------|
| InvestmentCreated event | Triggers holdFunds | Ignored by payment-service | ✅ Compatible (remove consumer) |
| InvestmentApproved event | Triggers captureFunds | Used for notifications only | ✅ Compatible (remove consumer) |
| PaymentCompleted event | Triggers wallet + investment update | Same behavior | ✅ Fully compatible |

**Answer: YES, RabbitMQ can still be used**

### Consumer Validity

```
❌ DELETE: InvestmentCreatedConsumer in payment-service
❌ DELETE: InvestmentApprovedConsumer in payment-service
✅ KEEP: PaymentCompletedConsumer in wallet-service
✅ KEEP: PaymentCompletedConsumer in investment-service
```

### Event Flow Comparison

```
BEFORE:
InvestmentCreated → payment-service → holdFunds
InvestmentApproved → payment-service → captureFunds → PaymentCompleted

AFTER:
[User action] → payment-service → Razorpay → PaymentCompleted
```

---

## STEP 7 — FINAL MIGRATION PLAN

### What to DELETE

```java
// payment-service

1. Methods:
   ❌ holdFunds()
   ❌ captureFunds()
   ❌ Any Stripe/mock payment gateway logic

2. Event Consumers:
   ❌ InvestmentCreatedConsumer
   ❌ InvestmentApprovedConsumer

3. Endpoints (if any):
   ❌ POST /payments/hold
   ❌ POST /payments/capture
```

### What to MODIFY

```java
// payment-service

1. Payment Entity:
   ✏️ Add: razorpayOrderId, razorpayPaymentId, razorpaySignature
   ✏️ Remove: transactionId (or repurpose)
   ✏️ Update: PaymentStatus enum

2. PaymentService:
   ✏️ Remove automatic payment logic
   ✏️ Keep: publishPaymentCompleted() method
```

### What to ADD

```java
// payment-service

1. Dependencies:
   ➕ Razorpay Java SDK
   <dependency>
       <groupId>com.razorpay</groupId>
       <artifactId>razorpay-java</artifactId>
       <version>1.4.3</version>
   </dependency>

2. Configuration:
   ➕ application.properties:
   razorpay.key.id=rzp_test_xxx
   razorpay.key.secret=xxx

3. New Endpoints:
   ➕ POST /api/payments/create-order
   ➕ POST /api/payments/confirm
   ➕ POST /api/payments/webhook (optional)

4. New Service Methods:
   ➕ createRazorpayOrder(investmentId, amount)
   ➕ confirmPayment(orderId, paymentId, signature)
   ➕ verifySignature(orderId, paymentId, signature)

5. New DTOs:
   ➕ CreateOrderRequest
   ➕ CreateOrderResponse
   ➕ ConfirmPaymentRequest
```

---

## MIGRATION STEPS (ORDERED)

### Phase 1: Preparation (No Breaking Changes)

```
Step 1: Add Razorpay dependency to payment-service
Step 2: Add Razorpay configuration (key, secret)
Step 3: Update Payment entity schema (add new fields, keep old)
Step 4: Run database migration
```

### Phase 2: Add New Endpoints (Parallel to Old System)

```
Step 5: Implement createRazorpayOrder() method
Step 6: Implement POST /api/payments/create-order endpoint
Step 7: Implement verifySignature() method
Step 8: Implement confirmPayment() method
Step 9: Implement POST /api/payments/confirm endpoint
Step 10: Test new flow end-to-end (without removing old system)
```

### Phase 3: Remove Old System (Breaking Changes)

```
Step 11: Delete InvestmentCreatedConsumer in payment-service
Step 12: Delete InvestmentApprovedConsumer in payment-service
Step 13: Delete holdFunds() method
Step 14: Delete captureFunds() method
Step 15: Remove old PaymentStatus values (HELD, CAPTURED, TRANSFERRED)
Step 16: Update PaymentStatus enum to new values
```

### Phase 4: Verification

```
Step 17: Run testPlan.md tests (update for new flow)
Step 18: Verify:
   - Investment creation does NOT trigger payment
   - Approval does NOT trigger capture
   - Manual payment flow works
   - Wallet credit still works via events
   - Investment completion still works via events
```

---

## PROBLEMS WITH CURRENT FLOW (Relative to Razorpay)

| Problem | Impact | Solution |
|---------|--------|----------|
| Payment auto-triggered on creation | User has no control | Remove InvestmentCreatedConsumer |
| Capture auto-triggered on approval | No user payment action | Remove InvestmentApprovedConsumer |
| Hold/capture pattern | Not compatible with Razorpay checkout | Replace with order/payment pattern |
| No Razorpay order ID storage | Cannot track Razorpay transactions | Add razorpayOrderId field |
| No user-initiated payment endpoint | Frontend cannot trigger payment | Add POST /payments/create-order |
| Mock payment gateway | Not real payment | Integrate Razorpay SDK |

---

## COMPONENTS TO REMOVE

```
payment-service/
├── consumers/
│   ├── InvestmentCreatedConsumer.java ❌ DELETE
│   └── InvestmentApprovedConsumer.java ❌ DELETE
├── service/
│   └── PaymentService.java
│       ├── holdFunds() ❌ DELETE
│       └── captureFunds() ❌ DELETE
└── gateway/
    └── MockPaymentGateway.java ❌ DELETE (or replace)
```

---

## NEW FLOW DESIGN (SUMMARY)

```
OLD: Investment → Auto Hold → Approval → Auto Capture → Wallet
NEW: Investment → Approval → User Pays (Razorpay) → Wallet
```

---

## REQUIRED API CHANGES

### New Endpoints

```
POST /api/payments/create-order
- Input: { investmentId }
- Output: { orderId, amount, currency }
- Auth: Required (investor only)

POST /api/payments/confirm
- Input: { razorpayOrderId, razorpayPaymentId, razorpaySignature }
- Output: { status, investmentId }
- Auth: Required (investor only)

POST /api/payments/webhook
- Input: Razorpay webhook payload
- Output: 200 OK
- Auth: Webhook signature verification
```

### Modified Behavior

```
POST /api/investments
- BEFORE: Triggers payment hold
- AFTER: Only creates investment (PENDING)

PUT /api/investments/{id}/approve
- BEFORE: Triggers payment capture
- AFTER: Only updates status (APPROVED)
```

---

## MINIMAL RAZORPAY INTEGRATION DESIGN

### Configuration

```java
@Configuration
public class RazorpayConfig {
    @Value("${razorpay.key.id}")
    private String keyId;
    
    @Value("${razorpay.key.secret}")
    private String keySecret;
    
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}
```

### Service Implementation

```java
@Service
public class RazorpayService {
    
    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    
    public CreateOrderResponse createOrder(String investmentId, BigDecimal amount) {
        // 1. Create Razorpay order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount.multiply(new BigDecimal(100)).intValue()); // paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", investmentId);
        
        Order order = razorpayClient.orders.create(orderRequest);
        
        // 2. Save payment record
        Payment payment = new Payment();
        payment.setInvestmentId(investmentId);
        payment.setAmount(amount);
        payment.setRazorpayOrderId(order.get("id"));
        payment.setStatus(PaymentStatus.INITIATED);
        paymentRepository.save(payment);
        
        // 3. Return order details
        return new CreateOrderResponse(order.get("id"), amount, "INR");
    }
    
    public void confirmPayment(String orderId, String paymentId, String signature) {
        // 1. Verify signature
        String payload = orderId + "|" + paymentId;
        boolean isValid = Utils.verifySignature(payload, signature, keySecret);
        if (!isValid) throw new InvalidSignatureException();
        
        // 2. Update payment
        Payment payment = paymentRepository.findByRazorpayOrderId(orderId);
        payment.setRazorpayPaymentId(paymentId);
        payment.setRazorpaySignature(signature);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
        
        // 3. Publish event
        publishPaymentCompleted(payment);
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @PostMapping("/create-order")
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        return razorpayService.createOrder(request.getInvestmentId(), request.getAmount());
    }
    
    @PostMapping("/confirm")
    public ConfirmPaymentResponse confirm(@RequestBody ConfirmPaymentRequest request) {
        razorpayService.confirmPayment(
            request.getRazorpayOrderId(),
            request.getRazorpayPaymentId(),
            request.getRazorpaySignature()
        );
        return new ConfirmPaymentResponse("SUCCESS");
    }
}
```

---

## MIGRATION CHECKLIST

```
☐ Add Razorpay SDK dependency
☐ Configure Razorpay keys
☐ Update Payment entity + migration
☐ Implement createOrder endpoint
☐ Implement confirm endpoint
☐ Test new flow (parallel to old)
☐ Delete InvestmentCreatedConsumer
☐ Delete InvestmentApprovedConsumer
☐ Delete holdFunds/captureFunds
☐ Update PaymentStatus enum
☐ Update testPlan.md
☐ Test idempotency (duplicate confirm calls)
☐ Test wallet credit via events
☐ Test investment completion via events
☐ Deploy payment-service
☐ Deploy frontend changes (Razorpay checkout)
```

---

**END OF AUDIT & MIGRATION PLAN**
