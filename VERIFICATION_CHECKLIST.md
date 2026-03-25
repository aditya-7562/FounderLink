# Verification Checklist - getUsersByRole Changes

## ✅ Changes Summary

### Modified Files
1. ✅ `User-Service/controller/UserController.java` - Changed from header to path variable
2. ✅ `notification-service/client/UserServiceClient.java` - Updated Feign client

### No Changes Needed
- ✅ `notification-service/service/NotificationService.java` - Already passes role as parameter
- ✅ `messaging-service` - Doesn't use getUsersByRole
- ✅ Other services - Don't use getUsersByRole

---

## 🧪 Testing Checklist

### Unit Tests (Optional)
```
☐ Test getUsersByRole with valid role (INVESTOR)
☐ Test getUsersByRole with valid role (FOUNDER)
☐ Test getUsersByRole with ADMIN role (should return 403)
☐ Test getUsersByRole with invalid role (should return 400)
```

### Integration Tests

#### User-Service Direct
```bash
☐ GET http://localhost:8081/users/role/INVESTOR
   Expected: 200 OK with list of investors

☐ GET http://localhost:8081/users/role/FOUNDER
   Expected: 200 OK with list of founders

☐ GET http://localhost:8081/users/role/ADMIN
   Expected: 403 Forbidden with empty list

☐ GET http://localhost:8081/users/role/INVALID
   Expected: 400 Bad Request with empty list
```

#### Notification Service Integration
```bash
☐ POST http://localhost:8083/startup (create startup)
   Expected: 
   - Startup created successfully
   - Notification service calls getUsersByRole("INVESTOR")
   - Emails sent to all investors
   - Check logs for "Sending startup created email to X investors"
```

---

## 🔍 Verification Steps

### Step 1: Check User-Service Endpoint
```bash
# Old endpoint (should not work anymore)
curl -X GET http://localhost:8081/users/role \
  -H "X-User-Role: INVESTOR"

# New endpoint (should work)
curl -X GET http://localhost:8081/users/role/INVESTOR
```

### Step 2: Check Notification Service Logs
```bash
# Create a startup and check logs
# Should see:
# "Sending startup created email to X investors"
# "Startup created emails sent to X investors"
```

### Step 3: Verify No Errors
```bash
# Check User-Service logs
# Should NOT see:
# - "Missing request header 'X-User-Role'"
# - Any 400/500 errors on /users/role/{role}

# Check Notification-Service logs
# Should NOT see:
# - Feign client errors
# - "Error fetching users from User-Service"
```

---

## 🚨 Common Issues & Solutions

### Issue 1: 404 Not Found on /users/role/INVESTOR
**Cause**: User-Service not restarted  
**Solution**: Restart User-Service

### Issue 2: Feign client error in Notification-Service
**Cause**: Notification-Service not restarted  
**Solution**: Restart Notification-Service

### Issue 3: Empty list returned
**Cause**: No users with that role in database  
**Solution**: Create test users with INVESTOR/FOUNDER roles

### Issue 4: 403 Forbidden for non-ADMIN roles
**Cause**: Logic error in controller  
**Solution**: Check if condition `if (roleEnum == Role.ADMIN)` is correct

---

## 📊 Expected Behavior

### Scenario 1: Query Investors
```
Request: GET /users/role/INVESTOR
Response: 200 OK
Body: [
  {
    "id": 1,
    "name": "Investor 1",
    "email": "investor1@example.com",
    "role": "INVESTOR"
  },
  ...
]
```

### Scenario 2: Query Founders
```
Request: GET /users/role/FOUNDER
Response: 200 OK
Body: [
  {
    "id": 2,
    "name": "Founder 1",
    "email": "founder1@example.com",
    "role": "FOUNDER"
  },
  ...
]
```

### Scenario 3: Query Admins (Blocked)
```
Request: GET /users/role/ADMIN
Response: 403 Forbidden
Body: []
```

### Scenario 4: Invalid Role
```
Request: GET /users/role/INVALID
Response: 400 Bad Request
Body: []
```

### Scenario 5: Notification Service Usage
```
Trigger: Create Startup
Action: Notification service calls getUsersByRole("INVESTOR")
Result: All investors receive email notification
```

---

## ✅ Sign-Off Checklist

```
☐ User-Service changes deployed
☐ Notification-Service changes deployed
☐ Manual testing completed
☐ No errors in logs
☐ Notification emails working
☐ Documentation updated
☐ API documentation updated (if exists)
☐ Postman collection updated (if exists)
```

---

## 🎯 Success Criteria

✅ **User-Service**:
- GET /users/role/{role} works for INVESTOR and FOUNDER
- GET /users/role/ADMIN returns 403
- Invalid roles return 400

✅ **Notification-Service**:
- Startup creation triggers investor emails
- No Feign client errors
- getUsersByRole("INVESTOR") works correctly

✅ **No Breaking Changes**:
- Other services continue to work
- No 500 errors
- No missing dependency errors

---

## 📝 Notes

- The old endpoint `GET /users/role` with header is **no longer supported**
- All clients must use the new endpoint `GET /users/role/{role}`
- ADMIN role queries are blocked for security
- Role names are case-insensitive (INVESTOR, investor, Investor all work)
- "ROLE_" prefix is automatically stripped (ROLE_INVESTOR → INVESTOR)

---

**Status**: ✅ READY FOR TESTING  
**Last Updated**: $(date)
