# Comprehensive Payment Workflow Analysis
**FounderLink Microservices System**

I've completed a thorough analysis of your codebase. Here's the complete plan:

---

## Step 1: System Understanding ✓

### What Actually Exists

- Investment Service manages investment proposals (not transactions)
- Users can create investments with amounts (in `BigDecimal`)
- Founders can approve/reject investments via status transitions (`PENDING → APPROVED → REJECTED → COMPLETED`)
- RabbitMQ infrastructure exists (`InvestmentEventPublisher` publishes `InvestmentCreatedEvent`)
- **NO** payment processing, gateway, or wallet system currently exists

### Natural Payment Use Case Found

**Strongest real-world workflow:**

1. **INVESTOR** submits investment proposal (amount, startup)
2. **FOUNDER** reviews and **APPROVES** the investment
3. On approval → funds should move from investor account to startup account ← *This is where payment is needed*
4. If **REJECTED** → funds should not be charged

This is a textbook Saga pattern use case because:
- Multiple services involved (Investment, Payment, Wallet, Startup)
- Distributed transaction across services
- Failure requires compensation (release funds if transfer fails)

---

## Step 2: Payment Integration Decision ✓

**Integration Point:** Investment Approval (`PUT /investments/{id}/status`)

**Why this moment?**

- **Investment CREATED** (`PENDING`) = advisor hasn't confirmed intent yet; nothing happens
- **Investment APPROVED** = founder's binding decision to accept funds → **THIS triggers payment**
- **Investment REJECTED** = compensation trigger (release any held funds)
- **Investment COMPLETED** = result of successful payment

---

## Step 3: Saga Design ✓

### Chosen: Orchestration-Based Saga

**Orchestrator:** `PaymentOrchestrator` (inside Payment Service)

**Why not choreography?**
- Choreography (event-driven) is implicit and hard to debug
- Investment approval is a decision, not just data
- Founder retains explicit control over fund movement
- Compensating transactions need clear sequencing

**Why orchestration?**
- ✓ Explicit state machine
- ✓ Clear failure handling (if capture fails, auto-release funds)
- ✓ Auditable step-by-step history
- ✓ Easy to extend (add tax calculation, compliance checks later)

---

## Step 4: Architecture Design ✓

### New Services Required

#### 1. Payment Service (Port 8088) — NEW

**Responsibilities:**
- Hold investor funds when investment created (authorizes but doesn't charge)
- Capture funds when investment approved (actually charges investor)
- Release funds when investment rejected or saga fails
- Transfer funds to startup wallet
- Manage idempotency (prevent double-charges on retries)
- Maintain payment transaction ledger (audit trail)

**Database Schema:**

```
PAYMENTS
├─ id, investmentId, investorId, startupId
├─ amount (BigDecimal)
├─ paymentStatus (PENDING_HOLD → HELD → CAPTURED → RELEASED/TRANSFERRED)
├─ idempotencyKey (UUID for deduplication)
└─ externalPaymentId (Stripe charge ID)

PAYMENT_TRANSACTION_LOG
├─ id, paymentId
├─ action (HOLD_SUCCESS, CAPTURE_SUCCESS, RELEASE_SUCCESS, etc.)
├─ details (JSON - error messages, gateway responses)
└─ timestamp
```

**APIs:**

```
POST /payments/{investmentId}/hold
  Input: investorId, startupId, amount, idempotencyKey
  Output: paymentId, status=PENDING_HOLD

PUT /payments/{paymentId}/capture
  Input: idempotencyKey
  Output: paymentId, status=CAPTURED

PUT /payments/{paymentId}/release
  Input: idempotencyKey, reason
  Output: paymentId, status=RELEASED

GET /payments/{paymentId}
  Output: Payment + transaction history
```

#### 2. Wallet Service (Port 8089) — NEW (Minimal MVP)

**Responsibility:**
- Hold startup wallets (balance = total funds raised)
- Accept deposits from Payment Service
- Query startup balance

**Database Schema:**

```
WALLETS
├─ id, startupId
├─ balance (BigDecimal)
└─ createdAt, updatedAt
```

**APIs:**

```
POST /wallets/{startupId}/deposit
  Input: amount, sourcePaymentId, idempotencyKey
  Output: walletId, balance (updated)

GET /wallets/{startupId}
  Output: walletId, startupId, balance
```

### Modified Services

#### Investment Service (Update)

Add to `InvestmentResponseDto`:

```java
String paymentStatus;         // PENDING_HOLD, HELD, CAPTURED, RELEASED, TRANSFERRED, FAILED
String paymentFailureReason;  // If hold/capture failed
```

Add to service:

```java
void markPaymentFailed(investmentId, reason);
PaymentStatusDto getPaymentStatus(investmentId);  // FeignClient to Payment Service
```

#### API Gateway (Update routing)

```yaml
- id: payment-service
  uri: lb://payment-service
  predicates: Path=/payments/**
  filters: AuthenticationFilter

- id: wallet-service
  uri: lb://wallet-service
  predicates: Path=/wallets/**
  filters: AuthenticationFilter
```

---

## Step 5: End-to-End Saga Flow ✓

### SUCCESS PATH: Investor → Approval → Funds Transfer

```
1. INVESTOR submits investment
   └─> POST /investments { startupId, amount }
   └─> Investment stored with status=PENDING
   └─> PublishEvent: InvestmentCreatedEvent

2. PaymentOrchestrator listens to InvestmentCreatedEvent
   └─> Calls: POST /payments/{investmentId}/hold
   └─> Stripe API: Authorize funds (don't charge yet)
   └─> Payment status: PENDING_HOLD

3. FOUNDER approves investment
   └─> PUT /investments/{id}/status { status: APPROVED }
   └─> PublishEvent: InvestmentApprovedEvent

4. PaymentOrchestrator listens to InvestmentApprovedEvent

   STEP A: Capture funds
   └─> PUT /payments/{paymentId}/capture
   └─> Stripe API: Charge investor
   └─> Payment status: CAPTURED
   └─> OnFailure → RELEASE (compensation)

   STEP B: Transfer to startup wallet
   └─> POST /wallets/{startupId}/deposit
   └─> SQL: UPDATE wallets SET balance = balance + amount
   └─> OnFailure → RELEASE captured funds (reverse charge)

   STEP C: Mark investment complete
   └─> Investment status: COMPLETED
```

**RESULT:**
- ✓ Investor account: -$50,000
- ✓ Startup wallet: +$50,000
- ✓ All transactions logged

---

### REJECTION PATH

```
FOUNDER rejects investment
  └─> PUT /investments/{id}/status { status: REJECTED }
  └─> PublishEvent: InvestmentRejectedEvent

PaymentOrchestrator listens
  └─> RELEASE held funds
  └─> Stripe API: Void authorization
  └─> Payment status: RELEASED
```

**RESULT:**
- ✓ Investor NOT charged
- ✓ Funds released

---

### FAILURE SCENARIOS

**Capture Fails (Stripe down)**
- Held funds exist
- Orchestrator catches exception → calls release
- Investor not charged
- Founder sees error, can retry

**Wallet Deposit Fails (network timeout)**
- Funds captured (investor charged)
- Orchestrator retries with exponential backoff
- Idempotency key prevents double-deposit
- If persistent: releases funds + marks `TRANSFER_FAILED`

**Orchestrator Crashes (mid-saga)**
- `InvestmentApprovedEvent` remains in RabbitMQ queue
- On restart, event replayed with SAME `idempotencyKey`
- Idempotency cache hit → no double-processing
- Saga resumes automatically

---

## Step 6: Integration Points ✓

### Modified Files

| File | Change |
|------|--------|
| `investment-service/.../InvestmentResponseDto.java` | Add `paymentStatus`, `paymentFailureReason` fields |
| `investment-service/.../InvestmentService.java` | Add `markPaymentFailed()`, `getPaymentStatus()` methods |
| `api-gateway/.../GatewayConfig.java` | Add routes for `payment-service` and `wallet-service` |

### New Services (Full Structure Below)

**Payment Service Directory Structure:**

```
payment-service/
├── pom.xml
├── application.yml
├── src/main/java/com/founderlink/payment/
│   ├── PaymentServiceApplication.java
│   ├── controller/
│   │   └── PaymentController.java
│   ├── service/
│   │   ├── PaymentService.java (interface)
│   │   └── PaymentServiceImpl.java
│   ├── saga/
│   │   └── InvestmentPaymentSagaOrchestrator.java  ← CORE
│   ├── entity/
│   │   ├── Payment.java
│   │   └── PaymentTransactionLog.java
│   ├── dto/
│   │   ├── request/PaymentHoldRequestDto.java
│   │   └── response/PaymentResponseDto.java
│   ├── client/
│   │   ├── PaymentGatewayClient.java (interface)
│   │   ├── StripePaymentGatewayImpl.java
│   │   └── MockPaymentGatewayImpl.java  ← for testing
│   ├── repository/
│   │   ├── PaymentRepository.java
│   │   └── PaymentTransactionLogRepository.java
│   ├── exception/
│   │   ├── PaymentGatewayException.java
│   │   └── InsufficientFundsException.java
│   └── config/
│       └── RabbitMQConfig.java
```

**Wallet Service Directory Structure:**

```
wallet-service/
├── pom.xml
├── application.yml
├── src/main/java/com/founderlink/wallet/
│   ├── WalletServiceApplication.java
│   ├── controller/
│   │   └── WalletController.java
│   ├── service/
│   │   ├── WalletService.java
│   │   └── WalletServiceImpl.java
│   ├── entity/
│   │   └── Wallet.java
│   ├── dto/
│   │   ├── request/DepositRequestDto.java
│   │   └── response/WalletResponseDto.java
│   └── repository/
│       └── WalletRepository.java
```

---

## Step 7: Data Consistency & Failures ✓

### Idempotency Strategy

**Problem:** Network retry could double-charge investor

**Solution: Idempotency Key Pattern**

```
Client generates: idempotencyKey = UUID.randomUUID()

Server on receipt:
  1. Check Redis cache: redis.get(idempotencyKey)
  2. If exists → return cached response (no duplicate action)
  3. If not → execute transaction
  4. Store response → redis.set(idempotencyKey, response, TTL=24h)
  5. Return response

Retry scenario:
  • First request: Hold $50k → paymentId=P123
  • Network timeout
  • Retry with SAME idempotencyKey
  • Server finds cached response → returns P123 (no double-hold)
```

### Failure Recovery Matrix

| Failure Point | Current State | Action | Compensation |
|---------------|--------------|--------|--------------|
| Hold fails | No investment created | Fail creation | None (no funds touched) |
| Capture fails | Investment APPROVED, funds held | Log error | Release held funds immediately |
| Deposit fails | Funds captured, wallet down | Retry 3x with backoff | Release captured funds if persistent |
| Orchestrator crashes | Mid-saga (after capture) | Event unacknowledged in RabbitMQ | Event replayed on restart w/ idempotency |

### Event Replay (RabbitMQ Persistence)

Orchestrator crashes between capture and deposit:

```
  Time T0: Capture successful → Payment.status=CAPTURED
  Time T1: Crash before deposit

  Time T2: Service restarts
  └─> Subscribe to "investment.approved" queue
  └─> Queue has unacknowledged message
  └─> Replay event with SAME idempotencyKey
  └─> Deposit call with cached idempotencyKey
  └─> Wallet detects duplicate → no double-deposit
```

**RESULT:** Saga completes correctly on restart

---

## Step 8: Final Architecture & Verification ✓

### Sequence Diagram (Happy Path)

```
INVESTOR            INVESTMENT           PAYMENT              WALLET
   │                  SERVICE            SERVICE            SERVICE
   │                    │                   │                  │
   ├─POST /invest──────>│                   │                  │
   │                    ├─Publish event────────────────────┐  │
   │<─201 CREATED───────┤                   │              ▼  │
   │ (PENDING)          │                   │ Hold funds       │
   │                    │                   │ (auth, no charge)│
   │                    │                   │ ✓ PENDING_HOLD   │
   │                    │                   │                  │
   │ [Founder reviews]  │                   │                  │
   │                    │                   │                  │
   ├─PUT /status────────>                   │                  │
   │ (APPROVED)         ├─Publish event────────────────────┐  │
   │<─200 OK────────────┤                   │              │  │
   │                    │                   │  Capture     │  │
   │                    │                   │  (charge now)│  │
   │                    │                   │ ✓ CAPTURED   │  │
   │                    │                   │              │  │
   │                    │                   │─deposit─────>│  │
   │                    │                   │              ▼  │
   │                    │                   │         Add $$ to
   │                    │                   │         startup balance
   │                    │                   │<─200 OK────────│
   │                    │<─update COMPLETED───────────────│
   │                    │ ✓                  │             │
   │                    │                    │             │
   └────────────────────────────────────────────────────────┘
           SAGA COMPLETE
        Investor: -$50,000
        Startup: +$50,000
```

### Service Responsibility Map

```
┌─────────────────────────────────────────────────────────┐
│  INVESTMENT & PAYMENT DOMAIN                             │
│                                                          │
│  Investment Service (8084)                               │
│  ├─ Manages investment proposals                        │
│  ├─ Founder: approve/reject                             │
│  └─ Publishes: InvestmentCreatedEvent, InvestmentApprovedEvent
│                                                          │
│  Payment Service ★ (8088) — NEW                        │
│  ├─ PaymentOrchestrator (listens to events)            │
│  ├─ Holds funds on investment creation                 │
│  ├─ Captures funds on founder approval                 │
│  ├─ Releases funds on rejection/failure                │
│  ├─ Idempotency management                             │
│  └─ FeignClient: Wallet Service (deposit)              │
│                                                          │
│  Wallet Service ★ (8089) — NEW                         │
│  ├─ Startup wallets (balance = funds raised)           │
│  └─ Receives deposits from Payment Service             │
│                                                          │
└─────────────────────────────────────────────────────────┘

★ = New services in this design
```

### Verification Checklist

- [ ] **Unit Tests:** Idempotency (5x same request = 1 charge)
- [ ] **Integration Tests:** Investment → Payment → Wallet event flow
- [ ] **End-to-End:** Create → Approve → Verify funds transferred
- [ ] **Failure Test:** Simulate capture failure → verify release
- [ ] **Load Test:** 1000 investments/min
- [ ] **Recovery Test:** Orchestrator crash → event replay → saga resumes

### Critical Design Decisions

| Decision | Rationale |
|----------|-----------|
| Orchestration | Explicit state machine for founder control; easier to debug than choreography |
| Separate Payment Service | Decouples finance logic; allows reuse for other payment scenarios |
| Hold on Create, Capture on Approve | Two-step flow gives visibility; aligns with real startup funding workflows |
| Idempotency via UUID | Prevents double-charges on network retries |
| RabbitMQ Events | System already uses RabbitMQ; no new tech stack |
| Startup Wallet Only | Investor wallets are future scope; MVP focuses on fund deposit to startup |

---

## Excluded (Out of Scope)

- ✗ Tax withholding (future: separate Tax Service)
- ✗ KYC/AML compliance checks (future: Compliance Service hooks)
- ✗ Investor wallets (future: Investor Accounting Service)
- ✗ Partial refunds (investment amount = atomic unit)
- ✗ Investment amendments (future: new flow)

---

## Next Steps (For Implementation)

1. Create Payment Service with `PaymentOrchestrator`
2. Create Wallet Service (minimal MVP)
3. Update Investment Service to add `paymentStatus` field
4. Configure RabbitMQ queues (`investment.created`, `investment.approved`, `investment.rejected`)
5. Implement Stripe adapter (`PaymentGatewayClient`)
6. Write integration tests for saga flow
7. Add idempotency cache (Redis)
8. Deploy with monitoring on payment saga steps
