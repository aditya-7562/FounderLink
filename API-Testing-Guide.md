# FounderLink — API Testing Guide

## Service Ports
| Service            | Port |
|--------------------|------|
| Auth Service       | 8089 |
| User Service       | 8081 |
| Startup Service    | 8083 |
| Team Service       | 8085 |
| Investment Service | 8084 |
| API Gateway        | 8090 |

---

## Test Users (Register these first, in order)

| # | Name           | Email                        | Password     | Role       |
|---|----------------|------------------------------|--------------|------------|
| 1 | Alice Founder  | alice@founderlink.com        | Alice@123    | FOUNDER    |
| 2 | Bob Cofounder  | bob@founderlink.com          | Bob@123      | COFOUNDER  |
| 3 | Carol Investor | carol@founderlink.com        | Carol@123    | INVESTOR   |

> After each login, copy the `accessToken` from the response — you'll need it as `Bearer <token>` in the `Authorization` header for all protected endpoints.

---

## 1. AUTH SERVICE — `http://localhost:8089`

---

### 1.1 Register — FOUNDER
```
POST /auth/register
Content-Type: application/json

{
  "name": "Alice Founder",
  "email": "alice@founderlink.com",
  "password": "Alice@123",
  "role": "FOUNDER"
}
```
Expected: `200 OK` with `userId`, `email`, `role`

---

### 1.2 Register — COFOUNDER
```
POST /auth/register
Content-Type: application/json

{
  "name": "Bob Cofounder",
  "email": "bob@founderlink.com",
  "password": "Bob@123",
  "role": "COFOUNDER"
}
```
Expected: `200 OK`

---

### 1.3 Register — INVESTOR
```
POST /auth/register
Content-Type: application/json

{
  "name": "Carol Investor",
  "email": "carol@founderlink.com",
  "password": "Carol@123",
  "role": "INVESTOR"
}
```
Expected: `200 OK`

---

### 1.4 Login — FOUNDER
```
POST /auth/login
Content-Type: application/json

{
  "email": "alice@founderlink.com",
  "password": "Alice@123"
}
```
Expected: `200 OK` with `accessToken`
> Save this token as **FOUNDER_TOKEN**
> The `refresh_token` is set as an HttpOnly cookie automatically.

---

### 1.5 Login — COFOUNDER
```
POST /auth/login
Content-Type: application/json

{
  "email": "bob@founderlink.com",
  "password": "Bob@123"
}
```
Expected: `200 OK` with `accessToken`
> Save this token as **COFOUNDER_TOKEN**

---

### 1.6 Login — INVESTOR
```
POST /auth/login
Content-Type: application/json

{
  "email": "carol@founderlink.com",
  "password": "Carol@123"
}
```
Expected: `200 OK` with `accessToken`
> Save this token as **INVESTOR_TOKEN**

---

### 1.7 Refresh Token
```
POST /auth/refresh
Cookie: refresh_token=<your_refresh_token_cookie>
```
Expected: `200 OK` with new `accessToken`

---

### 1.8 Logout
```
POST /auth/logout
Cookie: refresh_token=<your_refresh_token_cookie>
```
Expected: `204 No Content`, refresh_token cookie cleared

---

## 2. USER SERVICE — `http://localhost:8081`

> Note: Headers `X-User-Id` and `X-User-Role` are injected by the API Gateway in production.
> For direct testing, pass them manually.

---

### 2.1 Get User by ID
```
GET /users/1
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` with user details

---

### 2.2 Get All Users
```
GET /users
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` with list of users

---

### 2.3 Get Users by Role
```
GET /users/role
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` with users of that role

---

### 2.4 Update User
```
PUT /users/1
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "name": "Alice Updated",
  "email": "alice@founderlink.com"
}
```
Expected: `200 OK` with updated user

---

## 3. STARTUP SERVICE — `http://localhost:8083`

> Assumption: Alice (founderId = 1) is the FOUNDER.

---

### 3.1 Create Startup
```
POST /startup
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "name": "EduTech AI",
  "description": "AI-powered personalized learning platform",
  "industry": "EdTech",
  "problemStatement": "Students lack personalized learning paths",
  "solution": "AI engine that adapts content to each student",
  "fundingGoal": 500000.00,
  "stage": "MVP"
}
```
Expected: `201 Created`
> Save the returned `id` as **STARTUP_ID** (e.g. 1)

---

### 3.2 Get All Startups
```
GET /startup
X-User-Role: ROLE_INVESTOR
Authorization: Bearer <INVESTOR_TOKEN>
```
Expected: `200 OK` with list of startups

---

### 3.3 Get Startup by ID (Feign-compatible endpoint)
```
GET /startup/1
```
Expected: `200 OK` with startup details (no auth required — used by Feign clients)

---

### 3.4 Get Startup Details (Frontend endpoint)
```
GET /startup/details/1
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` wrapped in `ApiResponse`

---

### 3.5 Get Startups by Founder
```
GET /startup/founder
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` with Alice's startups

---

### 3.6 Search Startups
```
GET /startup/search?industry=EdTech&stage=MVP&minFunding=100000&maxFunding=1000000
X-User-Role: ROLE_INVESTOR
Authorization: Bearer <INVESTOR_TOKEN>
```
Expected: `200 OK` with filtered startups

---

### 3.7 Update Startup
```
PUT /startup/1
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "name": "EduTech AI v2",
  "description": "Updated AI-powered personalized learning platform",
  "industry": "EdTech",
  "problemStatement": "Students lack personalized learning paths",
  "solution": "Advanced AI engine with real-time feedback",
  "fundingGoal": 750000.00,
  "stage": "EARLY_TRACTION"
}
```
Expected: `200 OK` with updated startup

---

### 3.8 Delete Startup
```
DELETE /startup/1
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK`
> Only do this at the very end of testing — it triggers a RabbitMQ event that soft-deletes related investments and team members.

---

## 4. TEAM SERVICE — `http://localhost:8085`

> Pre-conditions: Startup (id=1) exists, Alice (id=1) is FOUNDER, Bob (id=2) is COFOUNDER.

---

### 4.1 Send Invitation (FOUNDER → COFOUNDER)
```
POST /teams/invite
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "startupId": 1,
  "invitedUserId": 2,
  "role": "CTO"
}
```
Expected: `201 Created`
> Save the returned `id` as **INVITATION_ID** (e.g. 1)

---

### 4.2 Get Invitations by Startup (FOUNDER view)
```
GET /teams/invitations/startup/1
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` with list of invitations for startup 1

---

### 4.3 Get Invitations by User (COFOUNDER view)
```
GET /teams/invitations/user
X-User-Id: 2
X-User-Role: ROLE_COFOUNDER
Authorization: Bearer <COFOUNDER_TOKEN>
```
Expected: `200 OK` with Bob's pending invitations

---

### 4.4 Reject Invitation (COFOUNDER)
```
PUT /teams/invitations/1/reject
X-User-Id: 2
X-User-Role: ROLE_COFOUNDER
Authorization: Bearer <COFOUNDER_TOKEN>
```
Expected: `200 OK` with status `REJECTED`
> After this, send a new invitation (repeat 4.1) to get a fresh PENDING invitation for the join test below.

---

### 4.5 Send Second Invitation (for join test)
```
POST /teams/invite
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "startupId": 1,
  "invitedUserId": 2,
  "role": "CTO"
}
```
Expected: `201 Created`
> Save new invitation id as **INVITATION_ID_2** (e.g. 2)

---

### 4.6 Cancel Invitation (FOUNDER)
```
PUT /teams/invitations/2/cancel
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` with status `CANCELLED`
> Send one more invitation (repeat 4.1) to get a fresh one for the join test.

---

### 4.7 Send Third Invitation (for join test)
```
POST /teams/invite
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "startupId": 1,
  "invitedUserId": 2,
  "role": "CTO"
}
```
Expected: `201 Created`
> Save as **INVITATION_ID_3** (e.g. 3)

---

### 4.8 Join Team (COFOUNDER accepts invitation)
```
POST /teams/join
X-User-Id: 2
X-User-Role: ROLE_COFOUNDER
Authorization: Bearer <COFOUNDER_TOKEN>
Content-Type: application/json

{
  "invitationId": 3
}
```
Expected: `201 Created`
> Save the returned team member `id` as **TEAM_MEMBER_ID** (e.g. 1)

---

### 4.9 Get Team by Startup ID
```
GET /teams/startup/1
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` with list of active team members

---

### 4.10 Get Member Work History (COFOUNDER)
```
GET /teams/member/history
X-User-Id: 2
X-User-Role: ROLE_COFOUNDER
Authorization: Bearer <COFOUNDER_TOKEN>
```
Expected: `200 OK` with all past and present team memberships for Bob

---

### 4.11 Get Active Member Roles (COFOUNDER)
```
GET /teams/member/active
X-User-Id: 2
X-User-Role: ROLE_COFOUNDER
Authorization: Bearer <COFOUNDER_TOKEN>
```
Expected: `200 OK` with Bob's currently active roles

---

### 4.12 Remove Team Member (FOUNDER)
```
DELETE /teams/1
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK`, team member soft-deleted (`isActive = false`)

---

## 5. INVESTMENT SERVICE — `http://localhost:8084`

> Pre-conditions: Startup (id=1) exists, Carol (id=3) is INVESTOR, Alice (id=1) is FOUNDER.

---

### 5.1 Create Investment (INVESTOR)
```
POST /investments
X-User-Id: 3
X-User-Role: ROLE_INVESTOR
Authorization: Bearer <INVESTOR_TOKEN>
Content-Type: application/json

{
  "startupId": 1,
  "amount": 50000.00
}
```
Expected: `201 Created`
> Save the returned `id` as **INVESTMENT_ID** (e.g. 1)

---

### 5.2 Get Investment by ID
```
GET /investments/1
X-User-Role: ROLE_INVESTOR
Authorization: Bearer <INVESTOR_TOKEN>
```
Expected: `200 OK` with investment details

---

### 5.3 Get Investments by Investor
```
GET /investments/investor
X-User-Id: 3
X-User-Role: ROLE_INVESTOR
Authorization: Bearer <INVESTOR_TOKEN>
```
Expected: `200 OK` with Carol's investment portfolio

---

### 5.4 Get Investments by Startup (FOUNDER)
```
GET /investments/startup/1
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
```
Expected: `200 OK` with all investments for startup 1

---

### 5.5 Update Investment Status — APPROVED
```
PUT /investments/1/status
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "status": "APPROVED"
}
```
Expected: `200 OK` with status `APPROVED`

---

### 5.6 Update Investment Status — COMPLETED
```
PUT /investments/1/status
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "status": "COMPLETED"
}
```
Expected: `200 OK` with status `COMPLETED`

---

### 5.7 Create Second Investment (for REJECTED test)
```
POST /investments
X-User-Id: 3
X-User-Role: ROLE_INVESTOR
Authorization: Bearer <INVESTOR_TOKEN>
Content-Type: application/json

{
  "startupId": 1,
  "amount": 25000.00
}
```
Expected: `201 Created`
> Save as **INVESTMENT_ID_2** (e.g. 2)

---

### 5.8 Update Investment Status — REJECTED
```
PUT /investments/2/status
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "status": "REJECTED"
}
```
Expected: `200 OK` with status `REJECTED`

---

## 6. Error / Edge Case Tests

These verify your exception handling and role guards are working correctly.

---

### 6.1 Wrong role — INVESTOR tries to create startup
```
POST /startup
X-User-Id: 3
X-User-Role: ROLE_INVESTOR
Authorization: Bearer <INVESTOR_TOKEN>
Content-Type: application/json

{
  "name": "Fake Startup",
  "description": "...",
  "industry": "FinTech",
  "problemStatement": "...",
  "solution": "...",
  "fundingGoal": 10000.00,
  "stage": "IDEA"
}
```
Expected: `403 Forbidden`

---

### 6.2 Startup not found
```
GET /startup/9999
```
Expected: `404 Not Found`

---

### 6.3 Duplicate investment — INVESTOR invests in same startup twice (while first is PENDING)
```
POST /investments
X-User-Id: 3
X-User-Role: ROLE_INVESTOR
Authorization: Bearer <INVESTOR_TOKEN>
Content-Type: application/json

{
  "startupId": 1,
  "amount": 10000.00
}
```
> Run this twice back to back before the founder approves/rejects.

Expected second call: `409 Conflict`

---

### 6.4 Invalid status transition — try to update a COMPLETED investment
```
PUT /investments/1/status
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "status": "APPROVED"
}
```
> Investment 1 is already COMPLETED from step 5.6

Expected: `409 Conflict` (InvalidStatusTransitionException)

---

### 6.5 COFOUNDER tries to send invitation (wrong role)
```
POST /teams/invite
X-User-Id: 2
X-User-Role: ROLE_COFOUNDER
Authorization: Bearer <COFOUNDER_TOKEN>
Content-Type: application/json

{
  "startupId": 1,
  "invitedUserId": 3,
  "role": "CPO"
}
```
Expected: `403 Forbidden`

---

### 6.6 Duplicate invitation — send same invitation twice
```
POST /teams/invite
X-User-Id: 1
X-User-Role: ROLE_FOUNDER
Authorization: Bearer <FOUNDER_TOKEN>
Content-Type: application/json

{
  "startupId": 1,
  "invitedUserId": 2,
  "role": "CPO"
}
```
> Run this twice while first is still PENDING.

Expected second call: `409 Conflict`

---

### 6.7 COFOUNDER tries to complete COMPLETED investment status update (wrong role)
```
PUT /investments/1/status
X-User-Id: 2
X-User-Role: ROLE_COFOUNDER
Authorization: Bearer <COFOUNDER_TOKEN>
Content-Type: application/json

{
  "status": "APPROVED"
}
```
Expected: `403 Forbidden`

---

## 7. Recommended Test Execution Order

```
1.  Register FOUNDER, COFOUNDER, INVESTOR         (Auth Service)
2.  Login all three users, save tokens             (Auth Service)
3.  Get users, update user                         (User Service)
4.  Create Startup                                 (Startup Service)
5.  Get all startups, search, get by id            (Startup Service)
6.  Send Invitation                                (Team Service)
7.  Get invitations (founder + cofounder views)    (Team Service)
8.  Reject invitation                              (Team Service)
9.  Send new invitation, cancel it                 (Team Service)
10. Send final invitation, join team               (Team Service)
11. Get team by startup, history, active roles     (Team Service)
12. Create Investment                              (Investment Service)
13. Get investment by id, by investor, by startup  (Investment Service)
14. Approve → Complete investment                  (Investment Service)
15. Create second investment, reject it            (Investment Service)
16. Remove team member                             (Team Service)
17. Run all error/edge case tests                  (All Services)
18. Delete Startup (last — triggers cascade event) (Startup Service)
19. Refresh token, logout                          (Auth Service)
```
