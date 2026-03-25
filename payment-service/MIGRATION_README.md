# Razorpay Migration - Implementation Complete

## Overview

The payment-service has been successfully migrated from Stripe-style hold/capture to Razorpay user-driven payment flow.

## What Changed

### 1. Payment Flow
- **OLD**: Automatic hold on investment creation → Automatic capture on approval
- **NEW**: User-initiated payment via Razorpay after approval

### 2. Payment Entity
- Removed: `externalPaymentId`
- Added: `razorpayOrderId`, `razorpayPaymentId`, `razorpaySignature`

### 3. Payment Status
- **OLD**: `PENDING_HOLD`, `HELD`, `CAPTURED`, `TRANSFERRED`, `RELEASED`, `FAILED`
- **NEW**: `INITIATED`, `SUCCESS`, `FAILED`

### 4. Removed Components
- `InvestmentCreatedConsumer` (no automatic hold)
- `InvestmentApprovedConsumer` (no automatic capture)
- `holdFunds()` method
- `captureFunds()` method
- `releaseFunds()` method

### 5. New Components
- `RazorpayService` - handles order creation and payment confirmation
- `RazorpayConfig` - Razorpay client configuration
- New DTOs: `CreateOrderRequest`, `CreateOrderResponse`, `ConfirmPaymentRequest`, `ConfirmPaymentResponse`
- New endpoints: `/payments/create-order`, `/payments/confirm`

## Database Migration

Run the migration script before starting the service:

```sql
-- Located at: src/main/resources/migration.sql
ALTER TABLE payments 
ADD COLUMN razorpay_order_id VARCHAR(100),
ADD COLUMN razorpay_payment_id VARCHAR(100),
ADD COLUMN razorpay_signature VARCHAR(500);

CREATE INDEX idx_razorpay_order_id ON payments(razorpay_order_id);
CREATE INDEX idx_razorpay_payment_id ON payments(razorpay_payment_id);
```

## Configuration

Add Razorpay credentials to `application.yml` or environment variables:

```yaml
razorpay:
  key:
    id: ${RAZORPAY_KEY_ID:rzp_test_dummy}
    secret: ${RAZORPAY_KEY_SECRET:dummy_secret}
```

Or set environment variables:
```bash
export RAZORPAY_KEY_ID=rzp_test_xxx
export RAZORPAY_KEY_SECRET=xxx
```

## New API Endpoints

### 1. Create Razorpay Order
```
POST /payments/create-order
Content-Type: application/json

{
  "investmentId": 123
}

Response:
{
  "orderId": "order_xxx",
  "amount": 10000,
  "currency": "INR",
  "investmentId": 123
}
```

### 2. Confirm Payment
```
POST /payments/confirm
Content-Type: application/json

{
  "razorpayOrderId": "order_xxx",
  "razorpayPaymentId": "pay_xxx",
  "razorpaySignature": "signature_hash"
}

Response:
{
  "status": "SUCCESS",
  "investmentId": 123
}
```

## Testing

1. Run database migration
2. Set Razorpay test credentials
3. Start all services (RabbitMQ, MySQL, investment-service, payment-service, wallet-service)
4. Follow the updated `testPlan.md` for end-to-end testing

## Event Flow

### Investment Created
- **OLD**: Triggered automatic payment hold
- **NEW**: No payment action (investment stays PENDING)

### Investment Approved
- **OLD**: Triggered automatic payment capture
- **NEW**: No payment action (investment stays APPROVED, waiting for user payment)

### Payment Completed
- **NEW**: Published after successful Razorpay payment confirmation
- Triggers: Wallet credit + Investment completion (unchanged)

## Backward Compatibility

- Old payment records remain in database
- GET endpoints still work for existing payments
- Event consumers for wallet and investment services unchanged

## Known Limitations

1. Signature verification uses basic Razorpay SDK method (not production-grade)
2. No webhook implementation (optional for future)
3. No refund flow
4. No payment status polling
5. Test mode only (requires valid Razorpay keys for production)

## Next Steps

1. ✅ Database migration
2. ✅ Configure Razorpay keys
3. ✅ Deploy payment-service
4. ⏳ Implement frontend Razorpay checkout integration
5. ⏳ Add webhook handler (optional)
6. ⏳ Production testing with real Razorpay account

## Support

For issues or questions, refer to:
- `migrationPlanning.md` - Complete migration plan
- `testPlan.md` - Updated test cases
- Razorpay documentation: https://razorpay.com/docs/

---

**Migration Status**: ✅ COMPLETE (Backend)
**Frontend Integration**: ⏳ PENDING
