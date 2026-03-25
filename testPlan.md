# End-to-End Test Plan: Investment-Payment-Wallet Microservices (Razorpay)

## 1. Services Setup

### Required Infrastructure
```
1. RabbitMQ
   - Port: 5672 (AMQP), 15672 (Management UI)
   - Start first

2. MySQL/PostgreSQL
   - Port: 3306 (MySQL) or 5432 (PostgreSQL)
   - Start second

3. investment-service
   - Port: 8081 (assumed)
   - Start third

4. payment-service
   - Port: 8088 (actual)
   - Start fourth

5. wallet-service
   - Port: 8083 (assumed)
   - Start fifth
```

### Environment Variables
```
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
DB_HOST=localhost
DB_PORT=3306
RAZORPAY_KEY_ID=rzp_test_xxx (set your test key)
RAZORPAY_KEY_SECRET=xxx (set your test secret)
```

### Startup Order
```
RabbitMQ → Database → investment-service → payment-service → wallet-service
```

---

## 2. Postman Collection Structure

**Collection Name:** `Investment-Payment-Wallet E2E Tests (Razorpay)`

### Folders:
```
├── 01_Happy_Path_Razorpay
├── 02_Idempotency_Tests
├── 03_Invalid_Transitions
├── 04_Failure_Scenarios
└── 99_Utilities (GET endpoints for verification)
```

---

## 3. Detailed Test Cases

### **FOLDER: 01_Happy_Path_Razorpay**

#### Test 1.1: Create Investment
```
Method: POST
URL: http://localhost:8081/api/investments
Headers: Content-Type: application/json
Body:
{
  "userId": "user123",
  "startupId": "startup456",
  "amount": 10000
}

Expected Response: 201 Created
{
  "id": "inv_001",
  "status": "PENDING",
  "amount": 10000
}

NOTE: NO automatic payment hold triggered
```

#### Test 1.2: Verify Investment Status
```
Method: GET
URL: http://localhost:8081/api/investments/{{investmentId}}

Expected Response: 200 OK
{
  "id": "inv_001",
  "status": "PENDING"
}
```

#### Test 1.3: Approve Investment
```
Method: PUT
URL: http://localhost:8081/api/investments/{{investmentId}}/approve

Expected Response: 200 OK
{
  "id": "inv_001",
  "status": "APPROVED"
}

NOTE: NO automatic payment capture triggered
```

#### Test 1.4: Verify Investment Still APPROVED (NOT COMPLETED)
```
Method: GET
URL: http://localhost:8081/api/investments/{{investmentId}}

Expected Response: 200 OK
{
  "id": "inv_001",
  "status": "APPROVED"
}

NOTE: Investment stays APPROVED until user pays
```

#### Test 1.5: Create Razorpay Order (USER ACTION)
```
Method: POST
URL: http://localhost:8088/payments/create-order
Headers: Content-Type: application/json
Body:
{
  "investmentId": {{investmentId}}
}

Expected Response: 201 Created
{
  "orderId": "order_xxx",
  "amount": 10000,
  "currency": "INR",
  "investmentId": {{investmentId}}
}

Save orderId to environment variable
```

#### Test 1.6: Simulate Razorpay Payment Success
```
Method: POST
URL: http://localhost:8088/payments/confirm
Headers: Content-Type: application/json
Body:
{
  "razorpayOrderId": "{{orderId}}",
  "razorpayPaymentId": "pay_test_123",
  "razorpaySignature": "dummy_signature_for_testing"
}

Expected Response: 200 OK
{
  "status": "SUCCESS",
  "investmentId": {{investmentId}}
}

NOTE: In production, signature must be valid
```

#### Test 1.7: Wait & Verify Investment Completion (after 2-5 seconds)
```
Method: GET
URL: http://localhost:8081/api/investments/{{investmentId}}

Expected Response: 200 OK
{
  "id": "inv_001",
  "status": "COMPLETED"
}
```

#### Test 1.8: Verify Payment Status
```
Method: GET
URL: http://localhost:8088/payments/investment/{{investmentId}}

Expected Response: 200 OK
{
  "investmentId": {{investmentId}},
  "status": "SUCCESS",
  "amount": 10000,
  "razorpayOrderId": "order_xxx",
  "razorpayPaymentId": "pay_test_123"
}
```

#### Test 1.9: Verify Wallet Credit
```
Method: GET
URL: http://localhost:8083/api/wallets/startup456

Expected Response: 200 OK
{
  "startupId": "startup456",
  "balance": 10000
}
```

---

### **FOLDER: 02_Idempotency_Tests**

#### Test 2.1: Duplicate Payment Confirmation
```
Step 1: Complete happy path (Tests 1.1 - 1.6)
Step 2: Call confirm AGAIN with same data

Method: POST
URL: http://localhost:8088/payments/confirm
Body: (same as Test 1.6)

Expected Response: 200 OK (idempotent)
{
  "status": "SUCCESS",
  "investmentId": {{investmentId}}
}

Step 3: Verify wallet balance

Expected: balance = 10000 (NOT 20000)
```

#### Test 2.2: Duplicate Order Creation
```
Step 1: Create investment and approve
Step 2: Create order (Test 1.5)
Step 3: Create order AGAIN for same investment

Method: POST
URL: http://localhost:8088/payments/create-order
Body: { "investmentId": {{investmentId}} }

Expected Response: 201 Created
Returns SAME orderId (idempotent)
```

#### Test 2.3: Multiple Investments Same Startup
```
Step 1: Complete happy path for investment 1
Step 2: Create NEW investment for SAME startup
Step 3: Approve and pay for investment 2
Step 4: Verify wallet balance

Expected: balance = 20000 (two separate investments)
```

---

### **FOLDER: 03_Invalid_Transitions**

#### Test 3.1: Reject After Completion
```
Step 1: Complete happy path (investment = COMPLETED)
Step 2: Attempt rejection

Method: PUT
URL: http://localhost:8081/api/investments/{{investmentId}}/reject

Expected Response: 
- 400 Bad Request OR
- 409 Conflict
{
  "error": "Cannot reject completed investment"
}

Step 3: Verify status unchanged

Expected: status = "COMPLETED"
```

#### Test 3.2: Create Order for Non-Approved Investment
```
Step 1: Create investment (PENDING)
Step 2: Try to create order WITHOUT approval

Method: POST
URL: http://localhost:8088/payments/create-order
Body: { "investmentId": {{investmentId}} }

Expected Response: 400/404 error
(Investment must be APPROVED first)
```

#### Test 3.3: Confirm Payment with Invalid Signature
```
Step 1: Create order
Step 2: Try to confirm with wrong signature

Method: POST
URL: http://localhost:8088/payments/confirm
Body:
{
  "razorpayOrderId": "{{orderId}}",
  "razorpayPaymentId": "pay_test_123",
  "razorpaySignature": "invalid_signature"
}

Expected Response: 400 Bad Request
{
  "error": "Invalid payment signature"
}
```

---

### **FOLDER: 04_Failure_Scenarios**

#### Test 4.1: Payment Service Down (Manual)
```
Step 1: Stop payment-service
Step 2: Create investment
Step 3: Approve investment

Expected: 200 OK, status = "APPROVED"

Step 4: Try to create order

Expected: Connection error (service unavailable)

Step 5: Restart payment-service
Step 6: Create order now

Expected: 201 Created (works after restart)
```

#### Test 4.2: Invalid Investment ID
```
Method: POST
URL: http://localhost:8088/payments/create-order
Body:
{
  "investmentId": 99999
}

Expected Response: 404 Not Found
{
  "error": "Investment not found: 99999"
}
```

#### Test 4.3: Wallet Service Down
```
Step 1: Stop wallet-service
Step 2: Complete payment flow (create order + confirm)

Expected: Payment confirmed but wallet credit fails

Step 3: Restart wallet-service
Step 4: Wait for event replay (or manually trigger)

Expected: Wallet eventually credited (eventual consistency)
```

---

### **FOLDER: 99_Utilities**

#### Get All Investments
```
Method: GET
URL: http://localhost:8081/api/investments
```

#### Get Wallet by Startup ID
```
Method: GET
URL: http://localhost:8083/api/wallets/{{startupId}}
```

#### Get Payment by Investment ID
```
Method: GET
URL: http://localhost:8088/payments/investment/{{investmentId}}
```

#### Get Payment by ID
```
Method: GET
URL: http://localhost:8088/payments/{{paymentId}}
```

---

## 4. Verification Strategy

### Per-Test Verification Matrix

| Test | HTTP Response | GET Investment | GET Payment | GET Wallet | DB Check |
|------|---------------|----------------|-------------|------------|----------|
| Happy Path | 200/201 | COMPLETED | SUCCESS | balance += amount | ✓ |
| Duplicate Confirm | 200 | COMPLETED | SUCCESS | balance NOT doubled | ✓ |
| Invalid Transition | 400/409 | status unchanged | - | - | ✓ |
| Service Down | Connection error → eventual success | APPROVED → COMPLETED | - | eventual credit | ✓ |

### Verification Steps (Generic)
```
1. Check HTTP status code
2. Check response body structure
3. GET investment status
4. GET payment status (if applicable)
5. GET wallet balance
6. Compare expected vs actual
```

### Failure Indicators
```
❌ HTTP 500 (server crash)
❌ Duplicate wallet credits
❌ Investment COMPLETED but payment INITIATED
❌ Wallet balance mismatch
❌ Invalid state transition accepted
❌ Payment confirmed without approval
```

---

## 5. Known Limitations

### Cannot Test via Postman:
```
❌ Real Razorpay signature verification (requires valid keys)
❌ Razorpay webhook callbacks (requires public URL)
❌ Direct RabbitMQ message replay
❌ Network partition simulation
❌ Concurrent race conditions (limited parallelism)
```

### Requires Manual Simulation:
```
⚠️ Service crashes (stop/start processes manually)
⚠️ Database connection loss
⚠️ RabbitMQ connection loss
⚠️ Real Razorpay payment flow (use Razorpay test mode)
```

### Approximations Used:
```
→ "Razorpay payment" = dummy signature (for testing only)
→ "Event replay" = sending duplicate API requests
→ "Service failure" = manual process stop/start
→ "Eventual consistency" = polling with delays
```

### Not Covered (Production Concerns):
```
- Real Razorpay signature verification
- Webhook security
- Authentication/Authorization
- Rate limiting
- Payment refunds
- Partial payments
```

---

## 6. Execution Checklist

### Pre-Test:
```
☐ All services running
☐ RabbitMQ queues empty
☐ Database clean state
☐ Razorpay test keys configured
☐ Postman environment variables set
```

### During Test:
```
☐ Record all response times
☐ Check RabbitMQ Management UI for queue depths
☐ Monitor service logs for errors
☐ Verify no automatic payment triggers
```

### Post-Test:
```
☐ Verify database state matches API responses
☐ Check for orphaned records
☐ Clear test data
☐ Document any anomalies
```

---

## 7. Quick Start Commands

### Postman Environment Variables
```json
{
  "investmentId": "",
  "orderId": "",
  "userId": "user123",
  "startupId": "startup456",
  "amount": 10000
}
```

### Set Variables from Response (Postman Test Script)
```javascript
// For investment creation
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("investmentId", jsonData.id);
});

// For order creation
pm.test("Order created", function () {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("orderId", jsonData.orderId);
});
```

### Wait Script (Postman Pre-request)
```javascript
setTimeout(function(){}, 3000); // 3 second delay
```

---

## 8. Key Differences from Old Flow

### OLD FLOW (Stripe-style):
```
1. Create Investment → AUTO hold funds
2. Approve Investment → AUTO capture funds
3. Wallet credited automatically
4. Investment marked COMPLETED
```

### NEW FLOW (Razorpay):
```
1. Create Investment → NO payment action
2. Approve Investment → NO payment action
3. User creates order (manual action)
4. User pays via Razorpay (manual action)
5. Confirm payment → triggers wallet credit
6. Investment marked COMPLETED
```

### Critical Changes:
```
✓ Payment is USER-INITIATED (not automatic)
✓ Investment stays APPROVED until payment
✓ No hold/capture pattern
✓ Direct payment via Razorpay
✓ Signature verification required
```

---

**END OF TEST PLAN**
