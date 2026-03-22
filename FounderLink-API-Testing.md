# FounderLink — Complete Postman API Testing Guide

> **Base URLs & Ports**

| Service | Port | Base URL |
|---------|------|----------|
| Service Registry (Eureka) | `8761` | `http://localhost:8761` |
| Auth Service | `8081` | `http://localhost:8081` |
| User Service | `8081` | `http://localhost:8081` (⚠️ port conflict with Auth) |
| Startup Service | `8083` | `http://localhost:8083` |
| Investment Service | `8084` | `http://localhost:8084` |
| Team Service | `8085` | `http://localhost:8085` |
| Messaging Service | `8086` | `http://localhost:8086` |
| Notification Service | `8087` | `http://localhost:8087` |

> ⚠️ **Note:** Auth Service and User Service both use port `8081`. Run them on different ports or one at a time when testing locally.

---

## Table of Contents

1. [Auth Service](#1-auth-service)
2. [User Service](#2-user-service)
3. [Startup Service](#3-startup-service)
4. [Investment Service](#4-investment-service)
5. [Team Service](#5-team-service)
6. [Messaging Service](#6-messaging-service)
7. [Notification Service](#7-notification-service)
8. [Inter-Service Communication Testing](#8-inter-service-communication-testing)
9. [End-to-End Flow Testing](#9-end-to-end-flow-testing)

---

## Common Headers

Most services require these headers for authentication/authorization:

| Header | Type | Description |
|--------|------|-------------|
| `X-User-Id` | `Long` | User ID extracted from JWT token |
| `X-User-Role` | `String` | User role (e.g., `ROLE_FOUNDER`, `ROLE_INVESTOR`, `ROLE_COFOUNDER`, `ROLE_ADMIN`) |
| `Content-Type` | `String` | `application/json` |
| `Authorization` | `String` | `Bearer <JWT_TOKEN>` (for Auth service protected endpoints) |

---

## 1. Auth Service

**Base URL:** `http://localhost:8081/auth`

### 1.1 Register User

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8081/auth/register` |
| **Content-Type** | `application/json` |

**Request Body — Register as FOUNDER:**

```json
{
  "name": "John Doe",
  "email": "john.founder@example.com",
  "password": "Password@123",
  "role": "FOUNDER"
}
```

**Request Body — Register as INVESTOR:**

```json
{
  "name": "Jane Smith",
  "email": "jane.investor@example.com",
  "password": "Password@123",
  "role": "INVESTOR"
}
```

**Request Body — Register as COFOUNDER:**

```json
{
  "name": "Bob Wilson",
  "email": "bob.cofounder@example.com",
  "password": "Password@123",
  "role": "COFOUNDER"
}
```

**Request Body — Register as ADMIN:**

```json
{
  "name": "Admin User",
  "email": "admin@founderlink.com",
  "password": "Admin@123",
  "role": "ADMIN"
}
```

**Expected Response (200 OK):**

```json
{
  "email": "john.founder@example.com",
  "role": "FOUNDER",
  "message": "User registered successfully"
}
```

---

### 1.2 Login

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8081/auth/login` |
| **Content-Type** | `application/json` |

**Request Body:**

```json
{
  "email": "john.founder@example.com",
  "password": "Password@123"
}
```

**Expected Response (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john.founder@example.com",
  "role": "FOUNDER",
  "userId": 1
}
```

> 📌 **Save the `token` and `userId` from the response — you'll need them for all subsequent requests.**  
> A `refresh_token` cookie will also be set in the response.

---

### 1.3 Refresh Token

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8081/auth/refresh` |
| **Cookie** | `refresh_token=<value_from_login_response_cookie>` |

**No request body required.** The refresh token is read from the HTTP-only cookie.

**Expected Response (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...(new_token)",
  "email": "john.founder@example.com",
  "role": "FOUNDER",
  "userId": 1
}
```

---

### 1.4 Logout

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8081/auth/logout` |
| **Cookie** | `refresh_token=<value_from_login_response_cookie>` |

**No request body required.**

**Expected Response (200 OK):** Empty body, refresh token cookie cleared.

---

## 2. User Service

**Base URL:** `http://localhost:8081/users`

### 2.1 Create User (Internal — Called by Auth Service)

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8081/users/internal` |
| **Content-Type** | `application/json` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-Auth-Source` | `gateway` |
| `X-Internal-Secret` | `my-founderlink-internal-secret-2024` |

**Request Body:**

```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.founder@example.com",
  "role": "FOUNDER",
  "skills": "Java, Spring Boot",
  "experience": "5 years",
  "bio": "Serial entrepreneur",
  "portfolioLinks": "https://github.com/johndoe"
}
```

**Expected Response (201 Created):**

```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.founder@example.com",
  "role": "FOUNDER",
  "skills": "Java, Spring Boot",
  "experience": "5 years",
  "bio": "Serial entrepreneur",
  "portfolioLinks": "https://github.com/johndoe"
}
```

---

### 2.2 Get User by ID

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8081/users/{id}` |

**Example:** `http://localhost:8081/users/1`

**Expected Response (200 OK):**

```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.founder@example.com",
  "role": "FOUNDER",
  "skills": "Java, Spring Boot",
  "experience": "5 years",
  "bio": "Serial entrepreneur",
  "portfolioLinks": "https://github.com/johndoe"
}
```

---

### 2.3 Update User

| Field | Value |
|-------|-------|
| **Method** | `PUT` |
| **URL** | `http://localhost:8081/users/{id}` |
| **Content-Type** | `application/json` |

**Example:** `http://localhost:8081/users/1`

**Request Body:**

```json
{
  "name": "John Doe Updated",
  "skills": "Java, Spring Boot, React",
  "experience": "6 years",
  "bio": "Serial entrepreneur & mentor",
  "portfolioLinks": "https://github.com/johndoe, https://linkedin.com/in/johndoe"
}
```

**Expected Response (200 OK):**

```json
{
  "userId": 1,
  "name": "John Doe Updated",
  "email": "john.founder@example.com",
  "role": "FOUNDER",
  "skills": "Java, Spring Boot, React",
  "experience": "6 years",
  "bio": "Serial entrepreneur & mentor",
  "portfolioLinks": "https://github.com/johndoe, https://linkedin.com/in/johndoe"
}
```

---

### 2.4 Get All Users

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8081/users` |

**Expected Response (200 OK):**

```json
[
  {
    "userId": 1,
    "name": "John Doe",
    "email": "john.founder@example.com",
    "role": "FOUNDER",
    "skills": "Java, Spring Boot",
    "experience": "5 years",
    "bio": "Serial entrepreneur",
    "portfolioLinks": "https://github.com/johndoe"
  },
  {
    "userId": 2,
    "name": "Jane Smith",
    "email": "jane.investor@example.com",
    "role": "INVESTOR",
    "skills": null,
    "experience": null,
    "bio": null,
    "portfolioLinks": null
  }
]
```

---

## 3. Startup Service

**Base URL:** `http://localhost:8083/startup`

### 3.1 Create Startup

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8083/startup` |
| **Content-Type** | `application/json` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (founder's user ID) |
| `X-User-Role` | `ROLE_FOUNDER` |

**Request Body:**

```json
{
  "name": "TechVision AI",
  "description": "AI-powered analytics platform for small businesses",
  "industry": "Artificial Intelligence",
  "problemStatement": "Small businesses lack access to affordable AI analytics tools",
  "solution": "Cloud-based AI analytics with affordable pricing tiers",
  "fundingGoal": 500000.00,
  "stage": "MVP"
}
```

**Expected Response (201 Created):**

```json
{
  "message": "Startup created successfully",
  "data": {
    "id": 1,
    "name": "TechVision AI",
    "description": "AI-powered analytics platform for small businesses",
    "industry": "Artificial Intelligence",
    "problemStatement": "Small businesses lack access to affordable AI analytics tools",
    "solution": "Cloud-based AI analytics with affordable pricing tiers",
    "fundingGoal": 500000.00,
    "stage": "MVP",
    "founderId": 1,
    "createdAt": "2026-03-22T10:30:00"
  }
}
```

**More Sample Startups:**

```json
{
  "name": "GreenEnergy Solutions",
  "description": "Renewable energy marketplace connecting producers and consumers",
  "industry": "Clean Energy",
  "problemStatement": "Difficulty in finding reliable renewable energy providers",
  "solution": "Marketplace platform with verified energy providers and transparent pricing",
  "fundingGoal": 1000000.00,
  "stage": "IDEA"
}
```

```json
{
  "name": "HealthTrack Pro",
  "description": "Wearable health monitoring with predictive diagnostics",
  "industry": "HealthTech",
  "problemStatement": "Delayed diagnosis of chronic conditions costs lives",
  "solution": "Real-time health monitoring with AI-based early warning system",
  "fundingGoal": 2000000.00,
  "stage": "EARLY_TRACTION"
}
```

```json
{
  "name": "EduBridge Connect",
  "description": "Online learning platform bridging education gaps in rural areas",
  "industry": "EdTech",
  "problemStatement": "Rural areas have limited access to quality education",
  "solution": "Low-bandwidth optimized learning platform with offline capabilities",
  "fundingGoal": 750000.00,
  "stage": "SCALING"
}
```

> **Valid `stage` values:** `IDEA`, `MVP`, `EARLY_TRACTION`, `SCALING`

---

### 3.2 Get Startup by ID

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8083/startup/{id}` |

**Example:** `http://localhost:8083/startup/1`

**No headers required** (used internally by Feign clients).

**Expected Response (200 OK):**

```json
{
  "message": "Startup fetched successfully",
  "data": {
    "id": 1,
    "name": "TechVision AI",
    "description": "AI-powered analytics platform for small businesses",
    "industry": "Artificial Intelligence",
    "problemStatement": "Small businesses lack access to affordable AI analytics tools",
    "solution": "Cloud-based AI analytics with affordable pricing tiers",
    "fundingGoal": 500000.00,
    "stage": "MVP",
    "founderId": 1,
    "createdAt": "2026-03-22T10:30:00"
  }
}
```

---

### 3.3 Get All Startups

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8083/startup` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Role` | `ROLE_INVESTOR` (or `ROLE_FOUNDER`, `ROLE_COFOUNDER`, `ROLE_ADMIN`) |

**Expected Response (200 OK):**

```json
{
  "message": "Startups fetched successfully",
  "data": [
    {
      "id": 1,
      "name": "TechVision AI",
      "description": "AI-powered analytics platform for small businesses",
      "industry": "Artificial Intelligence",
      "problemStatement": "Small businesses lack access to affordable AI analytics tools",
      "solution": "Cloud-based AI analytics with affordable pricing tiers",
      "fundingGoal": 500000.00,
      "stage": "MVP",
      "founderId": 1,
      "createdAt": "2026-03-22T10:30:00"
    }
  ]
}
```

---

### 3.4 Get Startups by Founder

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8083/startup/founder` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (founder's user ID) |
| `X-User-Role` | `ROLE_FOUNDER` |

**Expected Response (200 OK):**

```json
{
  "message": "Startups fetched successfully",
  "data": [
    {
      "id": 1,
      "name": "TechVision AI",
      "description": "AI-powered analytics platform for small businesses",
      "industry": "Artificial Intelligence",
      "problemStatement": "...",
      "solution": "...",
      "fundingGoal": 500000.00,
      "stage": "MVP",
      "founderId": 1,
      "createdAt": "2026-03-22T10:30:00"
    }
  ]
}
```

---

### 3.5 Update Startup

| Field | Value |
|-------|-------|
| **Method** | `PUT` |
| **URL** | `http://localhost:8083/startup/{id}` |
| **Content-Type** | `application/json` |

**Example:** `http://localhost:8083/startup/1`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (must be the founder who owns this startup) |
| `X-User-Role` | `ROLE_FOUNDER` |

**Request Body:**

```json
{
  "name": "TechVision AI Pro",
  "description": "AI-powered analytics platform for businesses of all sizes",
  "industry": "Artificial Intelligence",
  "problemStatement": "Businesses lack access to affordable AI analytics tools",
  "solution": "Cloud-based AI analytics with tiered pricing from startup to enterprise",
  "fundingGoal": 750000.00,
  "stage": "EARLY_TRACTION"
}
```

**Expected Response (200 OK):**

```json
{
  "message": "Startup updated successfully",
  "data": {
    "id": 1,
    "name": "TechVision AI Pro",
    "description": "AI-powered analytics platform for businesses of all sizes",
    "industry": "Artificial Intelligence",
    "problemStatement": "Businesses lack access to affordable AI analytics tools",
    "solution": "Cloud-based AI analytics with tiered pricing from startup to enterprise",
    "fundingGoal": 750000.00,
    "stage": "EARLY_TRACTION",
    "founderId": 1,
    "createdAt": "2026-03-22T10:30:00"
  }
}
```

---

### 3.6 Delete Startup (Soft Delete)

| Field | Value |
|-------|-------|
| **Method** | `DELETE` |
| **URL** | `http://localhost:8083/startup/{id}` |

**Example:** `http://localhost:8083/startup/1`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (must be the founder who owns this startup) |
| `X-User-Role` | `ROLE_FOUNDER` |

**Expected Response (200 OK):**

```json
{
  "message": "Startup deleted successfully",
  "data": null
}
```

---

### 3.7 Search Startups

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8083/startup/search` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Role` | `ROLE_INVESTOR` (or any valid role) |

**Query Parameters (all optional):**

| Parameter | Type | Example |
|-----------|------|---------|
| `industry` | String | `Artificial Intelligence` |
| `stage` | Enum | `MVP` |
| `minFunding` | BigDecimal | `100000` |
| `maxFunding` | BigDecimal | `1000000` |

**Example URLs:**

```
http://localhost:8083/startup/search?industry=Artificial Intelligence&stage=MVP
http://localhost:8083/startup/search?minFunding=100000&maxFunding=1000000
http://localhost:8083/startup/search?industry=HealthTech
http://localhost:8083/startup/search?stage=IDEA&minFunding=500000
```

**Expected Response (200 OK):**

```json
{
  "message": "Startups fetched successfully",
  "data": [
    {
      "id": 1,
      "name": "TechVision AI",
      "industry": "Artificial Intelligence",
      "stage": "MVP",
      "fundingGoal": 500000.00,
      "founderId": 1,
      "createdAt": "2026-03-22T10:30:00"
    }
  ]
}
```

---

## 4. Investment Service

**Base URL:** `http://localhost:8084/investments`

### 4.1 Create Investment

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8084/investments` |
| **Content-Type** | `application/json` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `2` (investor's user ID) |
| `X-User-Role` | `ROLE_INVESTOR` |

**Request Body:**

```json
{
  "startupId": 1,
  "amount": 50000.00
}
```

**More Sample Investments:**

```json
{
  "startupId": 1,
  "amount": 100000.00
}
```

```json
{
  "startupId": 2,
  "amount": 250000.00
}
```

```json
{
  "startupId": 3,
  "amount": 1000.00
}
```

> ⚠️ Minimum amount is `1000.00`

**Expected Response (201 Created):**

```json
{
  "message": "Investment created successfully",
  "data": {
    "id": 1,
    "startupId": 1,
    "investorId": 2,
    "amount": 50000.00,
    "status": "PENDING",
    "createdAt": "2026-03-22T11:00:00"
  }
}
```

> 📌 **Note:** This endpoint internally calls **Startup Service** via Feign to verify the startup exists. The Startup Service must be running.

---

### 4.2 Get Investment by ID

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8084/investments/{id}` |

**Example:** `http://localhost:8084/investments/1`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Role` | `ROLE_FOUNDER` (or `ROLE_INVESTOR`, `ROLE_ADMIN`) |

**Expected Response (200 OK):**

```json
{
  "message": "Investment fetched successfully",
  "data": {
    "id": 1,
    "startupId": 1,
    "investorId": 2,
    "amount": 50000.00,
    "status": "PENDING",
    "createdAt": "2026-03-22T11:00:00"
  }
}
```

---

### 4.3 Get Investments by Startup ID

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8084/investments/startup/{startupId}` |

**Example:** `http://localhost:8084/investments/startup/1`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (founder's user ID) |
| `X-User-Role` | `ROLE_FOUNDER` (or `ROLE_ADMIN`) |

**Expected Response (200 OK):**

```json
{
  "message": "Investments fetched successfully",
  "data": [
    {
      "id": 1,
      "startupId": 1,
      "investorId": 2,
      "amount": 50000.00,
      "status": "PENDING",
      "createdAt": "2026-03-22T11:00:00"
    },
    {
      "id": 2,
      "startupId": 1,
      "investorId": 3,
      "amount": 100000.00,
      "status": "APPROVED",
      "createdAt": "2026-03-22T11:30:00"
    }
  ]
}
```

---

### 4.4 Get Investments by Investor (My Investments)

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8084/investments/investor` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `2` (investor's user ID) |
| `X-User-Role` | `ROLE_INVESTOR` (or `ROLE_ADMIN`) |

**Expected Response (200 OK):**

```json
{
  "message": "Investments fetched successfully",
  "data": [
    {
      "id": 1,
      "startupId": 1,
      "investorId": 2,
      "amount": 50000.00,
      "status": "PENDING",
      "createdAt": "2026-03-22T11:00:00"
    }
  ]
}
```

---

### 4.5 Update Investment Status

| Field | Value |
|-------|-------|
| **Method** | `PUT` |
| **URL** | `http://localhost:8084/investments/{id}/status` |
| **Content-Type** | `application/json` |

**Example:** `http://localhost:8084/investments/1/status`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (founder's user ID — must own the startup) |
| `X-User-Role` | `ROLE_FOUNDER` (or `ROLE_ADMIN`) |

**Request Body — Approve:**

```json
{
  "status": "APPROVED"
}
```

**Request Body — Reject:**

```json
{
  "status": "REJECTED"
}
```

**Request Body — Complete:**

```json
{
  "status": "COMPLETED"
}
```

> **Valid status values:** `PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`, `STARTUP_CLOSED`

**Expected Response (200 OK):**

```json
{
  "message": "Investment status updated successfully",
  "data": {
    "id": 1,
    "startupId": 1,
    "investorId": 2,
    "amount": 50000.00,
    "status": "APPROVED",
    "createdAt": "2026-03-22T11:00:00"
  }
}
```

---

## 5. Team Service

**Base URL:** `http://localhost:8085/teams`

### 5.1 Send Invitation (Founder invites Co-founder)

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8085/teams/invite` |
| **Content-Type** | `application/json` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (founder's user ID) |
| `X-User-Role` | `ROLE_FOUNDER` |

**Request Body:**

```json
{
  "startupId": 1,
  "invitedUserId": 3,
  "role": "CTO"
}
```

**More Sample Invitations:**

```json
{
  "startupId": 1,
  "invitedUserId": 4,
  "role": "CPO"
}
```

```json
{
  "startupId": 1,
  "invitedUserId": 5,
  "role": "MARKETING_HEAD"
}
```

```json
{
  "startupId": 2,
  "invitedUserId": 3,
  "role": "ENGINEERING_LEAD"
}
```

> **Valid `role` values:** `CTO`, `CPO`, `MARKETING_HEAD`, `ENGINEERING_LEAD`

**Expected Response (201 Created):**

```json
{
  "message": "Invitation sent successfully",
  "data": {
    "id": 1,
    "startupId": 1,
    "founderId": 1,
    "invitedUserId": 3,
    "role": "CTO",
    "status": "PENDING",
    "createdAt": "2026-03-22T12:00:00",
    "updatedAt": null
  }
}
```

> 📌 **Note:** This endpoint internally calls **Startup Service** via Feign to verify the startup exists and the founder owns it.

---

### 5.2 Get Invitations by User (Co-founder views invitations)

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8085/teams/invitations/user` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `3` (co-founder's user ID) |
| `X-User-Role` | `ROLE_COFOUNDER` |

**Expected Response (200 OK):**

```json
{
  "message": "Invitations fetched successfully",
  "data": [
    {
      "id": 1,
      "startupId": 1,
      "founderId": 1,
      "invitedUserId": 3,
      "role": "CTO",
      "status": "PENDING",
      "createdAt": "2026-03-22T12:00:00",
      "updatedAt": null
    }
  ]
}
```

---

### 5.3 Get Invitations by Startup

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8085/teams/invitations/startup/{startupId}` |

**Example:** `http://localhost:8085/teams/invitations/startup/1`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (founder's user ID) |
| `X-User-Role` | `ROLE_FOUNDER` |

**Expected Response (200 OK):**

```json
{
  "message": "Invitations fetched successfully",
  "data": [
    {
      "id": 1,
      "startupId": 1,
      "founderId": 1,
      "invitedUserId": 3,
      "role": "CTO",
      "status": "PENDING",
      "createdAt": "2026-03-22T12:00:00",
      "updatedAt": null
    },
    {
      "id": 2,
      "startupId": 1,
      "founderId": 1,
      "invitedUserId": 4,
      "role": "CPO",
      "status": "PENDING",
      "createdAt": "2026-03-22T12:05:00",
      "updatedAt": null
    }
  ]
}
```

---

### 5.4 Join Team (Accept Invitation)

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8085/teams/join` |
| **Content-Type** | `application/json` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `3` (co-founder accepting the invitation) |
| `X-User-Role` | `ROLE_COFOUNDER` |

**Request Body:**

```json
{
  "invitationId": 1
}
```

**Expected Response (200 OK):**

```json
{
  "message": "Joined team successfully",
  "data": {
    "id": 1,
    "startupId": 1,
    "userId": 3,
    "role": "CTO",
    "isActive": true,
    "joinedAt": "2026-03-22T12:30:00",
    "leftAt": null
  }
}
```

---

### 5.5 Reject Invitation

| Field | Value |
|-------|-------|
| **Method** | `PUT` |
| **URL** | `http://localhost:8085/teams/invitations/{id}/reject` |

**Example:** `http://localhost:8085/teams/invitations/2/reject`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `4` (invited user rejecting) |
| `X-User-Role` | `ROLE_COFOUNDER` |

**Expected Response (200 OK):**

```json
{
  "message": "Invitation rejected successfully",
  "data": {
    "id": 2,
    "startupId": 1,
    "founderId": 1,
    "invitedUserId": 4,
    "role": "CPO",
    "status": "REJECTED",
    "createdAt": "2026-03-22T12:05:00",
    "updatedAt": "2026-03-22T12:35:00"
  }
}
```

---

### 5.6 Cancel Invitation (Founder cancels)

| Field | Value |
|-------|-------|
| **Method** | `PUT` |
| **URL** | `http://localhost:8085/teams/invitations/{id}/cancel` |

**Example:** `http://localhost:8085/teams/invitations/3/cancel`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (founder who sent the invitation) |
| `X-User-Role` | `ROLE_FOUNDER` |

**Expected Response (200 OK):**

```json
{
  "message": "Invitation cancelled successfully",
  "data": {
    "id": 3,
    "startupId": 1,
    "founderId": 1,
    "invitedUserId": 5,
    "role": "MARKETING_HEAD",
    "status": "CANCELLED",
    "createdAt": "2026-03-22T12:10:00",
    "updatedAt": "2026-03-22T12:40:00"
  }
}
```

---

### 5.7 Get Team by Startup ID

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8085/teams/startup/{startupId}` |

**Example:** `http://localhost:8085/teams/startup/1`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (any team viewer) |
| `X-User-Role` | `ROLE_FOUNDER` (or `ROLE_COFOUNDER`, `ROLE_INVESTOR`, `ROLE_ADMIN`) |

**Expected Response (200 OK):**

```json
{
  "message": "Team members fetched successfully",
  "data": [
    {
      "id": 1,
      "startupId": 1,
      "userId": 3,
      "role": "CTO",
      "isActive": true,
      "joinedAt": "2026-03-22T12:30:00",
      "leftAt": null
    }
  ]
}
```

---

### 5.8 Remove Team Member

| Field | Value |
|-------|-------|
| **Method** | `DELETE` |
| **URL** | `http://localhost:8085/teams/{teamMemberId}` |

**Example:** `http://localhost:8085/teams/1`

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` (founder of the startup) |
| `X-User-Role` | `ROLE_FOUNDER` |

**Expected Response (200 OK):**

```json
{
  "message": "Team member removed successfully",
  "data": null
}
```

---

### 5.9 Get Member History

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8085/teams/member/history` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `3` (co-founder's user ID) |
| `X-User-Role` | `ROLE_COFOUNDER` (or `ROLE_ADMIN`) |

**Expected Response (200 OK):**

```json
{
  "message": "Member history fetched successfully",
  "data": [
    {
      "id": 1,
      "startupId": 1,
      "userId": 3,
      "role": "CTO",
      "isActive": false,
      "joinedAt": "2026-03-22T12:30:00",
      "leftAt": "2026-03-22T14:00:00"
    }
  ]
}
```

---

### 5.10 Get Active Member Roles

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8085/teams/member/active` |

**Required Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `3` (co-founder's user ID) |
| `X-User-Role` | `ROLE_COFOUNDER` (or `ROLE_ADMIN`) |

**Expected Response (200 OK):**

```json
{
  "message": "Active roles fetched successfully",
  "data": [
    {
      "id": 2,
      "startupId": 2,
      "userId": 3,
      "role": "ENGINEERING_LEAD",
      "isActive": true,
      "joinedAt": "2026-03-22T15:00:00",
      "leftAt": null
    }
  ]
}
```

---

## 6. Messaging Service

**Base URL:** `http://localhost:8086/messages`

### 6.1 Send Message

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `http://localhost:8086/messages` |
| **Content-Type** | `application/json` |

**Request Body:**

```json
{
  "senderId": 1,
  "receiverId": 2,
  "content": "Hi Jane, I'd love to discuss the investment opportunity for TechVision AI."
}
```

**More Sample Messages:**

```json
{
  "senderId": 2,
  "receiverId": 1,
  "content": "Hi John! I'm interested. What's your current traction?"
}
```

```json
{
  "senderId": 1,
  "receiverId": 3,
  "content": "Hey Bob, would you like to join TechVision AI as CTO?"
}
```

```json
{
  "senderId": 3,
  "receiverId": 1,
  "content": "That sounds exciting! Let's set up a call to discuss the details."
}
```

**Expected Response (200 OK):**

```json
{
  "id": 1,
  "senderId": 1,
  "receiverId": 2,
  "content": "Hi Jane, I'd love to discuss the investment opportunity for TechVision AI.",
  "createdAt": "2026-03-22T13:00:00"
}
```

> 📌 **Note:** This endpoint internally calls **User Service** via Feign to verify both sender and receiver exist.

---

### 6.2 Get Message by ID

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8086/messages/{id}` |

**Example:** `http://localhost:8086/messages/1`

**Expected Response (200 OK):**

```json
{
  "id": 1,
  "senderId": 1,
  "receiverId": 2,
  "content": "Hi Jane, I'd love to discuss the investment opportunity for TechVision AI.",
  "createdAt": "2026-03-22T13:00:00"
}
```

---

### 6.3 Get Conversation Between Two Users

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8086/messages/conversation/{user1}/{user2}` |

**Example:** `http://localhost:8086/messages/conversation/1/2`

**Expected Response (200 OK):**

```json
[
  {
    "id": 1,
    "senderId": 1,
    "receiverId": 2,
    "content": "Hi Jane, I'd love to discuss the investment opportunity for TechVision AI.",
    "createdAt": "2026-03-22T13:00:00"
  },
  {
    "id": 2,
    "senderId": 2,
    "receiverId": 1,
    "content": "Hi John! I'm interested. What's your current traction?",
    "createdAt": "2026-03-22T13:05:00"
  }
]
```

---

### 6.4 Get Conversation Partners

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8086/messages/partners/{userId}` |

**Example:** `http://localhost:8086/messages/partners/1`

**Expected Response (200 OK):**

```json
[2, 3]
```

> Returns a list of user IDs that the given user has had conversations with.

---

## 7. Notification Service

**Base URL:** `http://localhost:8087/notifications`

### 7.1 Get All Notifications for User

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8087/notifications/{userId}` |

**Example:** `http://localhost:8087/notifications/1`

**Expected Response (200 OK):**

```json
[
  {
    "id": 1,
    "userId": 1,
    "type": "INVESTMENT",
    "message": "New investment of $50,000 received for TechVision AI",
    "read": false,
    "createdAt": "2026-03-22T11:00:00"
  },
  {
    "id": 2,
    "userId": 1,
    "type": "TEAM",
    "message": "Bob Wilson has joined TechVision AI as CTO",
    "read": false,
    "createdAt": "2026-03-22T12:30:00"
  }
]
```

---

### 7.2 Get Unread Notifications

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8087/notifications/{userId}/unread` |

**Example:** `http://localhost:8087/notifications/1/unread`

**Expected Response (200 OK):**

```json
[
  {
    "id": 1,
    "userId": 1,
    "type": "INVESTMENT",
    "message": "New investment of $50,000 received for TechVision AI",
    "read": false,
    "createdAt": "2026-03-22T11:00:00"
  }
]
```

---

### 7.3 Mark Notification as Read

| Field | Value |
|-------|-------|
| **Method** | `PUT` |
| **URL** | `http://localhost:8087/notifications/{id}/read` |

**Example:** `http://localhost:8087/notifications/1/read`

**Expected Response (200 OK):**

```json
{
  "id": 1,
  "userId": 1,
  "type": "INVESTMENT",
  "message": "New investment of $50,000 received for TechVision AI",
  "read": true,
  "createdAt": "2026-03-22T11:00:00"
}
```

---

## 8. Inter-Service Communication Testing

This section documents all service-to-service calls happening behind the scenes. These must be tested to ensure the microservices work together properly.

### 8.1 Auth Service → User Service (Feign Client)

| Field | Details |
|-------|---------|
| **Trigger** | `POST /auth/register` |
| **Internal Call** | Auth Service calls `POST /users/internal` on User Service |
| **Protocol** | Feign Client via Eureka discovery |
| **Circuit Breaker** | `userServiceSync` (Resilience4j) |

**How it works:**
1. User registers via Auth Service
2. Auth Service creates auth credentials (user + hashed password)
3. Auth Service calls User Service via Feign: `POST /users/internal`
4. User profile is created in User Service DB

**Headers sent by Feign:**

| Header | Value |
|--------|-------|
| `X-Auth-Source` | `gateway` |
| `X-Internal-Secret` | `my-founderlink-internal-secret-2024` |

**To test manually:**

```
POST http://localhost:8081/users/internal
Headers:
  X-Auth-Source: gateway
  X-Internal-Secret: my-founderlink-internal-secret-2024
  Content-Type: application/json
Body:
{
  "userId": 99,
  "name": "Test Internal User",
  "email": "test.internal@example.com",
  "role": "FOUNDER"
}
```

**Failure scenario (Circuit Breaker):**
- Stop User Service
- Register a user via Auth Service → Should trigger fallback
- Auth registration may succeed but user profile creation fails
- Circuit breaker opens after 50% failures in sliding window of 10

---

### 8.2 Investment Service → Startup Service (Feign Client)

| Field | Details |
|-------|---------|
| **Trigger** | `POST /investments` (Create Investment) |
| **Internal Call** | Investment Service calls `GET /startup/{id}` on Startup Service |
| **Protocol** | Feign Client via Eureka discovery |
| **Circuit Breaker** | `startup-service` (Resilience4j) |
| **Fallback** | `StartupServiceClientFallback` |

**How it works:**
1. Investor creates an investment for a startup
2. Investment Service calls Startup Service to verify startup exists
3. Investment Service also validates the startup is active (not deleted)
4. Investment Service validates the founder ID from startup response (for status updates)

**To test the inter-service call:**

**Step 1 — Ensure Startup Service is running and has data:**
```
POST http://localhost:8083/startup
Headers:
  X-User-Id: 1
  X-User-Role: ROLE_FOUNDER
  Content-Type: application/json
Body:
{
  "name": "Test Startup",
  "description": "Testing inter-service communication",
  "industry": "Technology",
  "problemStatement": "Test problem",
  "solution": "Test solution",
  "fundingGoal": 100000.00,
  "stage": "MVP"
}
```

**Step 2 — Create investment (triggers Feign call):**
```
POST http://localhost:8084/investments
Headers:
  X-User-Id: 2
  X-User-Role: ROLE_INVESTOR
  Content-Type: application/json
Body:
{
  "startupId": 1,
  "amount": 25000.00
}
```

**Failure scenario (Startup Service down):**
- Stop Startup Service
- Create an investment → Should trigger fallback and return error
- Circuit breaker opens after failures exceed threshold

---

### 8.3 Team Service → Startup Service (Feign Client)

| Field | Details |
|-------|---------|
| **Trigger** | `POST /teams/invite` (Send Invitation) |
| **Internal Call** | Team Service calls `GET /startup/{id}` on Startup Service |
| **Protocol** | Feign Client via Eureka discovery |
| **Circuit Breaker** | Resilience4j enabled |
| **Fallback** | `StartupServiceClientFallback` |

**How it works:**
1. Founder sends a team invitation
2. Team Service calls Startup Service to verify:
   - Startup exists
   - The requesting user is the founder of that startup (`founderId` match)
3. If valid, invitation is created

**To test the inter-service call:**

**Step 1 — Create a startup (if not already):**
```
POST http://localhost:8083/startup
Headers:
  X-User-Id: 1
  X-User-Role: ROLE_FOUNDER
Body: (same as above)
```

**Step 2 — Send invitation (triggers Feign call):**
```
POST http://localhost:8085/teams/invite
Headers:
  X-User-Id: 1
  X-User-Role: ROLE_FOUNDER
  Content-Type: application/json
Body:
{
  "startupId": 1,
  "invitedUserId": 3,
  "role": "CTO"
}
```

**Failure scenario (Startup Service down):**
- Stop Startup Service
- Send invitation → Should trigger fallback and return error

---

### 8.4 Messaging Service → User Service (Feign Client)

| Field | Details |
|-------|---------|
| **Trigger** | `POST /messages` (Send Message) |
| **Internal Call** | Messaging Service calls `GET /users/{id}` on User Service |
| **Protocol** | Feign Client via Eureka discovery |
| **Fallback** | `UserServiceClientFallback` |

**How it works:**
1. User sends a message
2. Messaging Service calls User Service to validate both sender and receiver exist
3. If both users exist, message is saved

**To test the inter-service call:**

**Step 1 — Ensure users exist in User Service:**
```
GET http://localhost:8081/users/1  → Should return user
GET http://localhost:8081/users/2  → Should return user
```

**Step 2 — Send message (triggers Feign call):**
```
POST http://localhost:8086/messages
Content-Type: application/json
Body:
{
  "senderId": 1,
  "receiverId": 2,
  "content": "Testing inter-service communication!"
}
```

**Failure scenario (User Service down):**
- Stop User Service
- Send a message → Should trigger fallback

---

### 8.5 Notification Service → User Service (Feign Client)

| Field | Details |
|-------|---------|
| **Trigger** | Internal notification creation via RabbitMQ events |
| **Internal Calls** | `GET /users/{id}` and `GET /users` on User Service |
| **Protocol** | Feign Client via Eureka discovery |
| **Fallback** | `UserServiceClientFallback` |

**How it works:**
1. Events are published to RabbitMQ queues by other services
2. Notification Service consumes events from queues
3. Notification Service calls User Service to get user details for notifications
4. Notifications are created and can be retrieved via REST API

---

### 8.6 Notification Service → Startup Service (Feign Client)

| Field | Details |
|-------|---------|
| **Trigger** | Internal notification creation via RabbitMQ events |
| **Internal Call** | `GET /startup/{id}` on Startup Service |
| **Protocol** | Feign Client via Eureka discovery |
| **Fallback** | `StartupServiceClientFallback` |

**How it works:**
1. When an investment or team event is received via RabbitMQ
2. Notification Service calls Startup Service to get startup name/details
3. Notification is created with meaningful message (e.g., "Investment of $50K received for TechVision AI")

---

### 8.7 RabbitMQ Event-Driven Communication

| Source Service | Exchange | Queue | Event Type |
|----------------|----------|-------|------------|
| Startup Service | `founderlink.exchange` | `startup.queue` | Startup created/updated/deleted |
| Investment Service | `founderlink.exchange` | `investment.queue` | Investment created/status changed |
| Team Service | `founderlink.exchange` | `team.queue` | Invitation sent/accepted/rejected, member added/removed |
| Messaging Service | `founderlink.exchange` | `messaging.queue` | New message sent |

**Consumer:** Notification Service listens to all queues and creates notifications.

**To test RabbitMQ flow:**
1. Start RabbitMQ server (`localhost:5672`)
2. Start Notification Service
3. Create a startup → Check if notification appears: `GET /notifications/{founderId}`
4. Create an investment → Check if founder gets notified
5. Send an invitation → Check if co-founder gets notified
6. Send a message → Check if receiver gets notified

**RabbitMQ Management UI:** `http://localhost:15672` (guest/guest)

---

## 9. End-to-End Flow Testing

### Flow 1: Complete Registration & Startup Creation

```
Step 1: Register Founder
  POST http://localhost:8081/auth/register
  Body: {"name":"Alice Founder","email":"alice@example.com","password":"Pass@123","role":"FOUNDER"}
  → Saves auth credentials + calls User Service internally

Step 2: Login as Founder
  POST http://localhost:8081/auth/login
  Body: {"email":"alice@example.com","password":"Pass@123"}
  → Save token & userId (e.g., userId=1)

Step 3: Create Startup
  POST http://localhost:8083/startup
  Headers: X-User-Id: 1, X-User-Role: ROLE_FOUNDER
  Body: {"name":"MyStartup","description":"Innovative platform","industry":"Fintech","problemStatement":"Complex finances","solution":"AI finance tool","fundingGoal":500000,"stage":"MVP"}
  → Save startupId (e.g., startupId=1)

Step 4: Verify Startup Created
  GET http://localhost:8083/startup/1
  → Should return the startup details
```

### Flow 2: Investor Registration & Investment

```
Step 1: Register Investor
  POST http://localhost:8081/auth/register
  Body: {"name":"Bob Investor","email":"bob@example.com","password":"Pass@123","role":"INVESTOR"}

Step 2: Login as Investor
  POST http://localhost:8081/auth/login
  Body: {"email":"bob@example.com","password":"Pass@123"}
  → Save token & userId (e.g., userId=2)

Step 3: Browse Startups
  GET http://localhost:8083/startup
  Headers: X-User-Role: ROLE_INVESTOR
  → See all available startups

Step 4: Search Startups
  GET http://localhost:8083/startup/search?industry=Fintech&stage=MVP
  Headers: X-User-Role: ROLE_INVESTOR
  → Filter startups

Step 5: Create Investment (triggers Feign → Startup Service)
  POST http://localhost:8084/investments
  Headers: X-User-Id: 2, X-User-Role: ROLE_INVESTOR
  Body: {"startupId":1,"amount":50000.00}
  → Save investmentId (e.g., investmentId=1)

Step 6: Founder Approves Investment
  PUT http://localhost:8084/investments/1/status
  Headers: X-User-Id: 1, X-User-Role: ROLE_FOUNDER
  Body: {"status":"APPROVED"}

Step 7: Investor Checks Their Investments
  GET http://localhost:8084/investments/investor
  Headers: X-User-Id: 2, X-User-Role: ROLE_INVESTOR  

Step 8: Check Notifications
  GET http://localhost:8087/notifications/1
  → Founder should see investment notification (if RabbitMQ is running)
```

### Flow 3: Team Building (Founder + Co-founder)

```
Step 1: Register Co-founder
  POST http://localhost:8081/auth/register
  Body: {"name":"Charlie CTO","email":"charlie@example.com","password":"Pass@123","role":"COFOUNDER"}
  → Save userId (e.g., userId=3)

Step 2: Founder Sends Invitation (triggers Feign → Startup Service)
  POST http://localhost:8085/teams/invite
  Headers: X-User-Id: 1, X-User-Role: ROLE_FOUNDER
  Body: {"startupId":1,"invitedUserId":3,"role":"CTO"}
  → Save invitationId (e.g., invitationId=1)

Step 3: Co-founder Views Invitations
  GET http://localhost:8085/teams/invitations/user
  Headers: X-User-Id: 3, X-User-Role: ROLE_COFOUNDER

Step 4: Co-founder Accepts (Join Team)
  POST http://localhost:8085/teams/join
  Headers: X-User-Id: 3, X-User-Role: ROLE_COFOUNDER
  Body: {"invitationId":1}

Step 5: View Team Members
  GET http://localhost:8085/teams/startup/1
  Headers: X-User-Id: 1, X-User-Role: ROLE_FOUNDER

Step 6: Check Notifications
  GET http://localhost:8087/notifications/1
  → Founder should see team join notification
  GET http://localhost:8087/notifications/3
  → Co-founder should see invitation notification
```

### Flow 4: Messaging Between Users

```
Step 1: Founder Messages Investor (triggers Feign → User Service)
  POST http://localhost:8086/messages
  Body: {"senderId":1,"receiverId":2,"content":"Hi Bob, thanks for the investment!"}

Step 2: Investor Replies
  POST http://localhost:8086/messages
  Body: {"senderId":2,"receiverId":1,"content":"Happy to be part of MyStartup!"}

Step 3: View Conversation
  GET http://localhost:8086/messages/conversation/1/2

Step 4: Check Conversation Partners
  GET http://localhost:8086/messages/partners/1
  → Should return [2]

Step 5: Check Notifications
  GET http://localhost:8087/notifications/2
  → Investor should see message notification (if RabbitMQ is running)
```

### Flow 5: Full Notification Flow

```
Step 1: Check all notifications for founder
  GET http://localhost:8087/notifications/1
  → Should see: startup created, investment received, team member joined, messages

Step 2: Check unread notifications
  GET http://localhost:8087/notifications/1/unread

Step 3: Mark as read
  PUT http://localhost:8087/notifications/1/read

Step 4: Verify read status
  GET http://localhost:8087/notifications/1/unread
  → Should no longer contain notification id=1
```

---

## Error Response Reference

### Common Error Responses

**400 Bad Request — Validation Error:**
```json
{
  "message": "Validation failed",
  "data": {
    "name": "Name is required",
    "email": "Invalid email format",
    "amount": "must be greater than or equal to 1000.00"
  }
}
```

**401 Unauthorized — Invalid/Missing Token:**
```json
{
  "message": "Unauthorized access"
}
```

**403 Forbidden — Wrong Role:**
```json
{
  "message": "Access denied. Only FOUNDER can perform this action"
}
```

**404 Not Found:**
```json
{
  "message": "Startup not found with id: 999",
  "data": null
}
```

**409 Conflict — Duplicate:**
```json
{
  "message": "User already exists with email: john@example.com"
}
```

**503 Service Unavailable — Circuit Breaker Open:**
```json
{
  "message": "Startup service is currently unavailable. Please try again later.",
  "data": null
}
```

---

## Quick Reference — All Endpoints

| # | Method | URL | Service | Required Role | Headers |
|---|--------|-----|---------|---------------|---------|
| 1 | POST | `/auth/register` | Auth | None | — |
| 2 | POST | `/auth/login` | Auth | None | — |
| 3 | POST | `/auth/refresh` | Auth | None | Cookie: refresh_token |
| 4 | POST | `/auth/logout` | Auth | None | Cookie: refresh_token |
| 5 | POST | `/users/internal` | User | Internal | X-Auth-Source, X-Internal-Secret |
| 6 | GET | `/users/{id}` | User | Any | — |
| 7 | PUT | `/users/{id}` | User | Any | — |
| 8 | GET | `/users` | User | Any | — |
| 9 | POST | `/startup` | Startup | FOUNDER | X-User-Id, X-User-Role |
| 10 | GET | `/startup/{id}` | Startup | Any/Feign | — |
| 11 | GET | `/startup` | Startup | Any Role | X-User-Role |
| 12 | GET | `/startup/founder` | Startup | FOUNDER | X-User-Id, X-User-Role |
| 13 | PUT | `/startup/{id}` | Startup | FOUNDER | X-User-Id, X-User-Role |
| 14 | DELETE | `/startup/{id}` | Startup | FOUNDER | X-User-Id, X-User-Role |
| 15 | GET | `/startup/search` | Startup | Any Role | X-User-Role + Query Params |
| 16 | POST | `/investments` | Investment | INVESTOR | X-User-Id, X-User-Role |
| 17 | GET | `/investments/{id}` | Investment | FOUNDER/INVESTOR/ADMIN | X-User-Role |
| 18 | GET | `/investments/startup/{id}` | Investment | FOUNDER/ADMIN | X-User-Id, X-User-Role |
| 19 | GET | `/investments/investor` | Investment | INVESTOR/ADMIN | X-User-Id, X-User-Role |
| 20 | PUT | `/investments/{id}/status` | Investment | FOUNDER/ADMIN | X-User-Id, X-User-Role |
| 21 | POST | `/teams/invite` | Team | FOUNDER | X-User-Id, X-User-Role |
| 22 | POST | `/teams/join` | Team | COFOUNDER | X-User-Id, X-User-Role |
| 23 | GET | `/teams/startup/{id}` | Team | Any Role | X-User-Id, X-User-Role |
| 24 | DELETE | `/teams/{id}` | Team | FOUNDER | X-User-Id, X-User-Role |
| 25 | GET | `/teams/member/history` | Team | COFOUNDER/ADMIN | X-User-Id, X-User-Role |
| 26 | GET | `/teams/member/active` | Team | COFOUNDER/ADMIN | X-User-Id, X-User-Role |
| 27 | GET | `/teams/invitations/user` | Team | COFOUNDER | X-User-Id, X-User-Role |
| 28 | GET | `/teams/invitations/startup/{id}` | Team | FOUNDER | X-User-Id, X-User-Role |
| 29 | PUT | `/teams/invitations/{id}/cancel` | Team | FOUNDER | X-User-Id, X-User-Role |
| 30 | PUT | `/teams/invitations/{id}/reject` | Team | COFOUNDER | X-User-Id, X-User-Role |
| 31 | POST | `/messages` | Messaging | None* | — |
| 32 | GET | `/messages/{id}` | Messaging | None* | — |
| 33 | GET | `/messages/conversation/{u1}/{u2}` | Messaging | None* | — |
| 34 | GET | `/messages/partners/{userId}` | Messaging | None* | — |
| 35 | GET | `/notifications/{userId}` | Notification | None* | — |
| 36 | GET | `/notifications/{userId}/unread` | Notification | None* | — |
| 37 | PUT | `/notifications/{id}/read` | Notification | None* | — |

> \* These endpoints don't enforce role headers at the controller level but may validate users via Feign calls internally.

---

## Inter-Service Communication Map

```
┌─────────────┐     Feign (POST /users/internal)      ┌──────────────┐
│ Auth Service │ ──────────────────────────────────────→│ User Service │
│   (8081)     │   Headers: X-Auth-Source,              │   (8081)     │
└─────────────┘   X-Internal-Secret                     └──────────────┘
                                                              ↑
                                                              │ Feign (GET /users/{id})
                                                              │
                                              ┌───────────────┴────────────────┐
                                              │                                │
                                    ┌─────────────────┐              ┌──────────────────┐
                                    │Messaging Service│              │Notification Svc  │
                                    │    (8086)       │              │     (8087)       │
                                    └─────────────────┘              └──────────────────┘
                                                                           ↑    ↑
                                                                           │    │ Feign (GET /startup/{id})
                                                            RabbitMQ       │    │
                                                            Events         │    │
┌─────────────────┐   Feign (GET /startup/{id})   ┌────────────────┐       │    │
│Investment Service│ ────────────────────────────→ │Startup Service │ ──────┘    │
│    (8084)       │                                │    (8083)      │ ───────────┘
└─────────────────┘                                └────────────────┘
                                                          ↑
┌──────────────┐   Feign (GET /startup/{id})              │
│ Team Service │ ─────────────────────────────────────────┘
│   (8085)     │
└──────────────┘

RabbitMQ Event Flow (all → Notification Service):
  startup-service    ──→ startup.queue    ──→ notification-service
  investment-service ──→ investment.queue ──→ notification-service
  team-service       ──→ team.queue      ──→ notification-service
  messaging-service  ──→ messaging.queue ──→ notification-service
```

---

## Startup Order

Start services in this order:
1. **Service-Registry** (Eureka) — Port 8761
2. **Config-Server** — Port 8888 (if used)
3. **RabbitMQ** — Port 5672
4. **Auth Service** — Port 8081
5. **User Service** — Port 8081 (⚠️ change to avoid conflict)
6. **Startup Service** — Port 8083
7. **Investment Service** — Port 8084
8. **Team Service** — Port 8085
9. **Messaging Service** — Port 8086
10. **Notification Service** — Port 8087

**Verify Eureka Dashboard:** `http://localhost:8761`
