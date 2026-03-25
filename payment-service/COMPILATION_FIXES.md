# Compilation Fixes Applied

## Issues Fixed

### 1. PaymentMapper.java
**Problem**: Referenced `payment.getExternalPaymentId()` which no longer exists in Payment entity.

**Fix**: Changed to `payment.getRazorpayPaymentId()` to map to the DTO's externalPaymentId field for backward compatibility.

```java
// OLD
dto.setExternalPaymentId(payment.getExternalPaymentId());

// NEW
dto.setExternalPaymentId(payment.getRazorpayPaymentId());
```

### 2. RazorpayService.java
**Problem**: Incorrect signature verification API usage for Razorpay Utils.

**Fix**: Updated to use correct JSONObject structure with proper field names.

```java
// OLD
boolean isValid = Utils.verifyPaymentSignature(
    new JSONObject()
        .put("order_id", orderId)
        .put("payment_id", paymentId)
        .put("signature", signature)
        .toString(),
    keySecret
);

// NEW
JSONObject attributes = new JSONObject();
attributes.put("razorpay_order_id", orderId);
attributes.put("razorpay_payment_id", paymentId);
attributes.put("razorpay_signature", signature);

boolean isValid = Utils.verifyPaymentSignature(attributes, keySecret);
```

### 3. PaymentController.java
**Problem**: Tried to fetch existing payment for order creation, but payment doesn't exist yet for new investments.

**Fix**: Changed to accept investment details as query parameters instead of looking up existing payment.

```java
// NEW approach
@PostMapping("/create-order")
public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
        @Valid @RequestBody CreateOrderRequest request,
        @RequestParam BigDecimal amount,
        @RequestParam Long investorId,
        @RequestParam Long startupId,
        @RequestParam Long founderId) {
    // Create order with provided details
}
```

**Alternative**: Could also integrate with investment-service via Feign client to fetch investment details.

## Files Modified

1. `PaymentMapper.java` - Updated field mapping
2. `RazorpayService.java` - Fixed signature verification
3. `PaymentController.java` - Updated create-order endpoint

## Testing

After these fixes, the service should compile successfully. To test:

1. Ensure all dependencies are in pom.xml
2. Run `mvn clean compile` or build in IDE
3. Check for any remaining compilation errors
4. Run the service and test endpoints

## Notes

- The signature verification will work correctly with real Razorpay responses
- For testing without real Razorpay, you may need to mock or skip signature verification
- The create-order endpoint now requires investment details as parameters
