# getUsersByRole API Change Summary

## Problem
The `getUsersByRole` method in User-Service was restricted:
- Took role from `X-User-Role` header (propagated by API Gateway)
- Only allowed users to find users with their own role
- Founders could only find founders, investors only investors, etc.

## Solution
Changed the API to accept role as a **path variable** instead of header, allowing anyone to query users by any role.

---

## Changes Made

### 1. User-Service Controller (UPDATED)

**File**: `User-Service/src/main/java/com/founderlink/User_Service/controller/UserController.java`

#### Before:
```java
@GetMapping("/role")
public ResponseEntity<List<UserResponseDto>> getUsersByRole(
        @RequestHeader(name = "X-User-Role") String roleHeader) {
    // Role taken from header
}
```

**Endpoint**: `GET /users/role`  
**Header Required**: `X-User-Role: INVESTOR`

#### After:
```java
@GetMapping("/role/{role}")
public ResponseEntity<List<UserResponseDto>> getUsersByRole(
        @PathVariable String role) {
    // Role taken from path variable
}
```

**Endpoint**: `GET /users/role/{role}`  
**Example**: `GET /users/role/INVESTOR`

---

### 2. Notification Service Feign Client (UPDATED)

**File**: `notification-service/src/main/java/com/founderlink/notification/client/UserServiceClient.java`

#### Before:
```java
@GetMapping("/users/role")
List<UserDTO> getUsersByRole(@RequestHeader("X-User-Role") String role);
```

#### After:
```java
@GetMapping("/users/role/{role}")
List<UserDTO> getUsersByRole(@PathVariable("role") String role);
```

**Usage in NotificationService** (no changes needed):
```java
List<UserDTO> investors = userServiceClient.getUsersByRole("INVESTOR");
```

This already passes "INVESTOR" as a parameter, so it works seamlessly with the new path variable approach.

---

## API Behavior

### Endpoint
```
GET /users/role/{role}
```

### Path Variable
- `role`: The role to filter by (INVESTOR, FOUNDER, ADMIN)

### Examples
```bash
# Get all investors
GET /users/role/INVESTOR

# Get all founders
GET /users/role/FOUNDER

# Get all admins (blocked - returns 403)
GET /users/role/ADMIN
```

### Response
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "INVESTOR"
  },
  {
    "id": 2,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "role": "INVESTOR"
  }
]
```

### Security
- ✅ Anyone can query INVESTOR or FOUNDER roles
- ❌ ADMIN role queries are blocked (returns 403 Forbidden)
- ✅ Invalid roles return 400 Bad Request

---

## Impact Analysis

### Services Using getUsersByRole

| Service | Usage | Impact | Status |
|---------|-------|--------|--------|
| notification-service | ✅ Uses via Feign | Updated Feign client | ✅ FIXED |
| messaging-service | ❌ Does not use | No impact | ✅ N/A |
| investment-service | ❌ Does not use | No impact | ✅ N/A |
| payment-service | ❌ Does not use | No impact | ✅ N/A |
| team-service | ❌ Does not use | No impact | ✅ N/A |

---

## Notification Service Usage

The notification service uses `getUsersByRole` in:

### 1. sendStartupCreatedEmailToAllInvestors
```java
List<UserDTO> investors = userServiceClient.getUsersByRole("INVESTOR");
```
**Purpose**: Send email to all investors when a new startup is created

### 2. No other usages found

---

## Testing

### Manual Testing

#### Test 1: Get Investors
```bash
GET http://localhost:8081/users/role/INVESTOR

Expected: List of all investors
```

#### Test 2: Get Founders
```bash
GET http://localhost:8081/users/role/FOUNDER

Expected: List of all founders
```

#### Test 3: Get Admins (Should Fail)
```bash
GET http://localhost:8081/users/role/ADMIN

Expected: 403 Forbidden with empty list
```

#### Test 4: Invalid Role
```bash
GET http://localhost:8081/users/role/INVALID

Expected: 400 Bad Request with empty list
```

#### Test 5: Notification Service Integration
```bash
# Create a startup (triggers notification to investors)
POST http://localhost:8083/startup

Expected: 
- Startup created
- Notification service fetches investors via getUsersByRole("INVESTOR")
- Emails sent to all investors
```

---

## Backward Compatibility

### Breaking Changes
❌ **Old endpoint no longer works**:
```
GET /users/role
Header: X-User-Role: INVESTOR
```

✅ **New endpoint**:
```
GET /users/role/INVESTOR
```

### Migration Required
Any service or client using the old endpoint must update to:
1. Change URL from `/users/role` to `/users/role/{role}`
2. Remove `X-User-Role` header
3. Pass role as path variable

---

## Benefits

### Before
- ❌ Restricted: Users could only find users with their own role
- ❌ Dependent on API Gateway header propagation
- ❌ Less flexible for cross-role queries

### After
- ✅ Open: Anyone can find users with any role (except ADMIN)
- ✅ Independent: No header dependency
- ✅ Flexible: Easy to query any role
- ✅ RESTful: Role is part of the URL path

---

## Security Considerations

### What's Protected
- ✅ ADMIN role queries are blocked (403 Forbidden)
- ✅ Invalid roles return 400 Bad Request

### What's Open
- ⚠️ Anyone can query INVESTOR or FOUNDER roles
- ⚠️ No authentication required (if you want to add auth, do it at API Gateway level)

### Recommendation
If you need to restrict who can query roles:
1. Add authentication at API Gateway
2. Add role-based access control (RBAC) in User-Service
3. Example: Only ADMIN can query all roles

---

## Files Modified

1. ✅ `User-Service/src/main/java/com/founderlink/User_Service/controller/UserController.java`
2. ✅ `notification-service/src/main/java/com/founderlink/notification/client/UserServiceClient.java`

---

## Status

✅ **Changes Complete**  
✅ **Notification Service Updated**  
✅ **No Breaking Changes in Other Services**  
✅ **Ready for Testing**

---

## Next Steps

1. ✅ Restart User-Service
2. ✅ Restart Notification-Service
3. ⏳ Test endpoints manually
4. ⏳ Test notification flow (startup creation → investor emails)
5. ⏳ Update API documentation
6. ⏳ Update Postman collection (if exists)
