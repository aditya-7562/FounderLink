# FounderLink — Notification & Email Testing Guide

Complete guide for testing notifications via RabbitMQ events, Postman API calls, and email verification.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [How Email Sending Works](#2-how-email-sending-works)
3. [How to Check If Emails Are Sent](#3-how-to-check-if-emails-are-sent)
4. [Gmail App Password Setup](#4-gmail-app-password-setup)
5. [RabbitMQ Event Flow Testing](#5-rabbitmq-event-flow-testing)
6. [Notification REST API Testing (Postman)](#6-notification-rest-api-testing-postman)
7. [End-to-End Notification Flows](#7-end-to-end-notification-flows)
8. [RabbitMQ Management UI](#8-rabbitmq-management-ui)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Prerequisites

Before testing notifications, ensure these services are running:

| Service | Port | Required |
|---------|------|----------|
| Service Registry (Eureka) | 8761 | ✅ Always |
| RabbitMQ Server | 5672 (AMQP) / 15672 (UI) | ✅ Always |
| MySQL Server | 3306 | ✅ Always |
| User Service | 8081 | ✅ For email lookups |
| Startup Service | 8083 | ✅ For startup events |
| Investment Service | 8084 | ✅ For investment events |
| Team Service | 8085 | ✅ For team invite events |
| Messaging Service | 8086 | ✅ For message events |
| **Notification Service** | **8087** | **✅ This is the service under test** |

**Create the database (if not exists):**

```sql
CREATE DATABASE IF NOT EXISTS notification_db;
```

**Create the table (MySQL 5.5 compatible):**

```sql
USE notification_db;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    is_read BIT DEFAULT 0,
    created_at DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

---

## 2. How Email Sending Works

The notification service uses **JavaMailSender** with Gmail SMTP to send emails. Here's the flow:

```
Producer Service (startup/investment/team/messaging)
        │
        ▼
   RabbitMQ Exchange (founderlink.exchange)
        │
        ▼
   Queue (startup.queue / investment.queue / team.queue / messaging.queue)
        │
        ▼
   Notification Service — EventConsumer
        │
        ├──► Creates in-app notification (saved to DB)
        │
        └──► Sends email via JavaMailSender (Gmail SMTP)
```

### Events → Notifications + Emails

| Event | Queue | In-App Notification | Email Sent To |
|-------|-------|-------------------|---------------|
| Startup Created | `startup.queue` | All users (investors) | All investor emails |
| Investment Created | `investment.queue` | Founder of the startup | Founder's email |
| Team Invite Sent | `team.queue` | Invited co-founder | Invited user's email |
| Message Sent | `messaging.queue` | Message receiver | ❌ No email (in-app only) |

---

## 3. How to Check If Emails Are Sent

### Option A: Use Mailtrap (Recommended for Testing)

[Mailtrap](https://mailtrap.io) is a **free fake SMTP server** that captures all emails without actually delivering them. Perfect for testing.

**Step 1:** Sign up at https://mailtrap.io (free tier)

**Step 2:** Get your SMTP credentials from the Mailtrap inbox:
- Host: `sandbox.smtp.mailtrap.io`
- Port: `2525`
- Username: `your-mailtrap-username`
- Password: `your-mailtrap-password`

**Step 3:** Update `notification-service/src/main/resources/application.properties`:

```properties
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=your-mailtrap-username
spring.mail.password=your-mailtrap-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

**Step 4:** Trigger any event → Check Mailtrap inbox at https://mailtrap.io to see the captured email.

---

### Option B: Use Real Gmail SMTP

If you want to send real emails via Gmail:

**Step 1:** Update `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-real-email@gmail.com
spring.mail.password=your-16-char-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

**Step 2:** Check the recipient's inbox (or spam folder) for the email.

> ⚠️ You **must** use a Gmail App Password, not your regular password. See Section 4.

---

### Option C: Check Service Logs

Even without a working SMTP server, you can verify the email *attempt* by checking the notification service logs:

**Success log:**
```
INFO  c.f.n.service.EmailService : Email sent successfully to: alice@example.com
```

**Failure log (SMTP not configured):**
```
ERROR c.f.n.service.EmailService : Failed to send email to alice@example.com: Mail server connection failed
```

> The in-app notifications are still created and saved to DB even if email sending fails.

---

### Option D: Check RabbitMQ Management UI

Open http://localhost:15672 (login: `guest` / `guest`)

- Go to **Queues** tab
- Check if messages were consumed (the "Messages" count drops to 0 after consumption)
- If messages are stuck in the queue, the notification service isn't consuming them

---

## 4. Gmail App Password Setup

Gmail blocks regular password authentication. You need an **App Password**:

1. Go to https://myaccount.google.com/security
2. Enable **2-Step Verification** (required)
3. Go to https://myaccount.google.com/apppasswords
4. Select app: **Mail**, device: **Other** (type "FounderLink")
5. Click **Generate** → Copy the 16-character password
6. Use this password in `spring.mail.password`

---

## 5. RabbitMQ Event Flow Testing

### 5.1 Test: Startup Created → Notification to All Investors

**Trigger:** Create a startup via Startup Service

```
POST http://localhost:8083/startup
```

**Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` |
| `X-User-Role` | `ROLE_FOUNDER` |
| `Content-Type` | `application/json` |

**Body:**

```json
{
  "name": "TechVision AI",
  "description": "AI-powered analytics platform",
  "industry": "Artificial Intelligence",
  "problemStatement": "Complex data analysis",
  "solution": "AI-driven insights",
  "fundingGoal": 500000.00,
  "stage": "MVP"
}
```

**What happens behind the scenes:**
1. Startup Service creates the startup
2. Startup Service publishes `StartupCreatedEvent` → `founderlink.exchange` with routing key `startup.created`
3. Notification Service consumes from `startup.queue`
4. Notification Service calls User Service (`GET /users`) to get all users
5. Creates an in-app notification for **every user**
6. Sends email to **all user emails** with subject: `🚀 New Startup Alert: TechVision AI`

**Verify:**

```
GET http://localhost:8087/notifications/1
```

**Expected notification:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "type": "STARTUP_CREATED",
    "message": "New startup 'TechVision AI' in Artificial Intelligence is now open for investment. Funding goal: $500,000.00",
    "read": false,
    "createdAt": "2026-03-22T15:30:00"
  }
]
```

**Check email (Mailtrap/Gmail):**
- Subject: `🚀 New Startup Alert: TechVision AI`
- Body contains: Startup Name, Industry, Funding Goal, Startup ID

**Check notification service logs:**
```
INFO  c.f.n.consumer.EventConsumer : Received STARTUP_CREATED event: {startupId=1, startupName=TechVision AI, ...}
INFO  c.f.n.service.NotificationService : Sending startup created email to X investors
INFO  c.f.n.service.EmailService : Email sent successfully to: bob@example.com
INFO  c.f.n.consumer.EventConsumer : Startup created emails sent for startup #1
```

---

### 5.2 Test: Investment Created → Notification to Founder

**Trigger:** Create an investment via Investment Service

```
POST http://localhost:8084/investments
```

**Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `2` |
| `X-User-Role` | `ROLE_INVESTOR` |
| `Content-Type` | `application/json` |

**Body:**

```json
{
  "startupId": 1,
  "amount": 50000.00
}
```

**What happens behind the scenes:**
1. Investment Service creates the investment
2. Investment Service publishes `InvestmentCreatedEvent` → `founderlink.exchange` with routing key `investment.created`
3. Event payload: `{ startupId, investorId, founderId, amount }`
4. Notification Service consumes from `investment.queue`
5. Notification Service calls User Service to get founder & investor details
6. Creates in-app notification for the **founder**
7. Sends email to **founder's email** with subject: `💡 New Investor Interest in Startup #1`

**Verify:**

```
GET http://localhost:8087/notifications/1
```

**Expected notification (for founder userId=1):**
```json
{
  "id": 2,
  "userId": 1,
  "type": "INVESTMENT_CREATED",
  "message": "Bob Investor showed investment interest in startup #1 with $50,000.00",
  "read": false,
  "createdAt": "2026-03-22T15:35:00"
}
```

**Check email:**
- Subject: `💡 New Investor Interest in Startup #1`
- Body contains: Investor Name, Interested Amount, Startup ID

---

### 5.3 Test: Team Invite Sent → Notification to Invited User

**Trigger:** Send a team invitation via Team Service

```
POST http://localhost:8085/teams/invite
```

**Headers:**

| Header | Value |
|--------|-------|
| `X-User-Id` | `1` |
| `X-User-Role` | `ROLE_FOUNDER` |
| `Content-Type` | `application/json` |

**Body:**

```json
{
  "startupId": 1,
  "invitedUserId": 3,
  "role": "CTO"
}
```

**What happens behind the scenes:**
1. Team Service creates the invitation
2. Team Service publishes `TeamInviteEvent` → `founderlink.exchange` with routing key `team.invite.sent`
3. Event payload: `{ startupId, invitedUserId, role }`
4. Notification Service consumes from `team.queue`
5. Creates in-app notification: `"You have been invited to join startup #1 as CTO"`
6. Calls User Service to get invited user's email
7. Sends email to **invited user** with subject: `🤝 Team Invitation for Startup #1`

**Verify:**

```
GET http://localhost:8087/notifications/3
```

**Expected notification (for invited userId=3):**
```json
{
  "id": 3,
  "userId": 3,
  "type": "TEAM_INVITE_SENT",
  "message": "You have been invited to join startup #1 as CTO",
  "read": false,
  "createdAt": "2026-03-22T15:40:00"
}
```

**Check email:**
- Subject: `🤝 Team Invitation for Startup #1`
- Body contains: User's name, Startup ID, Role (CTO)

---

### 5.4 Test: Message Sent → In-App Notification Only

**Trigger:** Send a message via Messaging Service

```
POST http://localhost:8086/messages
```

**Headers:**

| Header | Value |
|--------|-------|
| `Content-Type` | `application/json` |

**Body:**

```json
{
  "senderId": 1,
  "receiverId": 2,
  "content": "Hi Bob, thanks for the investment!"
}
```

**What happens behind the scenes:**
1. Messaging Service creates the message
2. Messaging Service publishes event → `founderlink.exchange` with routing key `message.sent`
3. Event payload: `{ messageId, senderId, receiverId, senderName }`
4. Notification Service consumes from `messaging.queue`
5. Creates **in-app notification only** (no email sent)

**Verify:**

```
GET http://localhost:8087/notifications/2
```

**Expected notification (for receiver userId=2):**
```json
{
  "id": 4,
  "userId": 2,
  "type": "MESSAGE_RECEIVED",
  "message": "You have a new message from Alice Founder",
  "read": false,
  "createdAt": "2026-03-22T15:45:00"
}
```

> ℹ️ No email is sent for message events — only an in-app notification is created.

---

## 6. Notification REST API Testing (Postman)

### 6.1 Get All Notifications for a User

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
    "type": "STARTUP_CREATED",
    "message": "New startup 'TechVision AI' in Artificial Intelligence is now open for investment. Funding goal: $500,000.00",
    "read": false,
    "createdAt": "2026-03-22T15:30:00"
  },
  {
    "id": 2,
    "userId": 1,
    "type": "INVESTMENT_CREATED",
    "message": "Bob Investor showed investment interest in startup #1 with $50,000.00",
    "read": false,
    "createdAt": "2026-03-22T15:35:00"
  }
]
```

---

### 6.2 Get Unread Notifications

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `http://localhost:8087/notifications/{userId}/unread` |

**Example:** `http://localhost:8087/notifications/1/unread`

**Expected Response (200 OK):**

```json
[
  {
    "id": 2,
    "userId": 1,
    "type": "INVESTMENT_CREATED",
    "message": "Bob Investor showed investment interest in startup #1 with $50,000.00",
    "read": false,
    "createdAt": "2026-03-22T15:35:00"
  }
]
```

---

### 6.3 Mark Notification as Read

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
  "type": "STARTUP_CREATED",
  "message": "New startup 'TechVision AI' in Artificial Intelligence is now open for investment. Funding goal: $500,000.00",
  "read": true,
  "createdAt": "2026-03-22T15:30:00"
}
```

---

## 7. End-to-End Notification Flows

### Flow 1: Full Startup + Investment + Notification

```
Step 1: Ensure all services are running

Step 2: Create a startup (triggers startup.created event)
  POST http://localhost:8083/startup
  Headers: X-User-Id: 1, X-User-Role: ROLE_FOUNDER
  Body: {"name":"MyStartup","description":"Test","industry":"Fintech","problemStatement":"P","solution":"S","fundingGoal":500000,"stage":"MVP"}

Step 3: Check notifications for all users
  GET http://localhost:8087/notifications/1
  GET http://localhost:8087/notifications/2
  → All users should have a STARTUP_CREATED notification
  → Check Mailtrap/Gmail for startup alert email

Step 4: Create an investment (triggers investment.created event)
  POST http://localhost:8084/investments
  Headers: X-User-Id: 2, X-User-Role: ROLE_INVESTOR
  Body: {"startupId":1,"amount":50000.00}

Step 5: Check founder's notifications
  GET http://localhost:8087/notifications/1
  → Founder should see INVESTMENT_CREATED notification
  → Check Mailtrap/Gmail for investment interest email

Step 6: Mark notification as read
  PUT http://localhost:8087/notifications/1/read
  → Notification should now show "read": true

Step 7: Check unread count
  GET http://localhost:8087/notifications/1/unread
  → Should not include the read notification
```

### Flow 2: Team Invite + Notification

```
Step 1: Send team invitation (triggers team.invite.sent event)
  POST http://localhost:8085/teams/invite
  Headers: X-User-Id: 1, X-User-Role: ROLE_FOUNDER
  Body: {"startupId":1,"invitedUserId":3,"role":"CTO"}

Step 2: Check invited user's notifications
  GET http://localhost:8087/notifications/3
  → Should see TEAM_INVITE_SENT notification
  → Check Mailtrap/Gmail for team invite email

Step 3: Check unread notifications
  GET http://localhost:8087/notifications/3/unread
  → Should list the unread invite notification
```

### Flow 3: Message + In-App Notification

```
Step 1: Send a message (triggers message.sent event)
  POST http://localhost:8086/messages
  Body: {"senderId":1,"receiverId":2,"content":"Hello!"}

Step 2: Check receiver's notifications
  GET http://localhost:8087/notifications/2
  → Should see MESSAGE_RECEIVED notification
  → NO email is sent for messages
```

---

## 8. RabbitMQ Management UI

**URL:** http://localhost:15672  
**Login:** `guest` / `guest`

### Key Things to Check

| Tab | What to Look For |
|-----|-----------------|
| **Overview** | Connections, Channels, Queues summary |
| **Queues** | `startup.queue`, `investment.queue`, `team.queue`, `messaging.queue` — check message counts |
| **Exchanges** | `founderlink.exchange` should exist as a `direct` exchange |
| **Connections** | Each running service should have a connection |

### Queue Health Indicators

| Queue State | Meaning |
|-------------|---------|
| Messages = 0 | ✅ All messages consumed successfully |
| Messages > 0 | ⚠️ Notification service isn't consuming (may be down) |
| Consumers = 0 | ❌ Notification service is not connected |
| Consumers = 1 | ✅ Notification service is listening |

### Manually Publish a Test Message (RabbitMQ UI)

You can test the consumer without running other services:

1. Go to **Exchanges** → click `founderlink.exchange`
2. Scroll to **Publish message**
3. Set **Routing key:** `startup.created`
4. Set **Properties:** `content_type = application/json`
5. Set **Payload:**

```json
{
  "startupId": 99,
  "startupName": "Test From RabbitMQ",
  "founderId": 1,
  "industry": "Testing",
  "fundingGoal": 100000.00
}
```

6. Click **Publish message**
7. Check: `GET http://localhost:8087/notifications/1` → Should show new notification

> This is useful for testing the notification service **in isolation** without needing the startup service running.

---

## 9. Troubleshooting

### No notifications appearing

| Problem | Solution |
|---------|----------|
| Notification service not consuming | Check if it's registered in Eureka (http://localhost:8761) |
| Queue doesn't exist | Restart all services — queues are auto-created on startup |
| Messages stuck in queue | Check notification service logs for consumer errors |
| Empty notifications list | Verify the `notifications` table exists in `notification_db` |

### Emails not sending

| Problem | Solution |
|---------|----------|
| `Mail server connection failed` | SMTP credentials are wrong or not configured |
| `Authentication failed` | Use Gmail App Password, not regular password |
| No email received at all | Check spam folder; or use Mailtrap instead |
| `your-email@gmail.com` in config | Replace with your actual Gmail address |
| Email sent but notification empty | User Service may be down (fallback returns empty user list) |

### Check notification service logs

Look for these log patterns in the terminal running notification-service:

```
✅ INFO  EventConsumer : Received STARTUP_CREATED event: {...}
✅ INFO  EmailService  : Email sent successfully to: user@example.com
❌ ERROR EmailService  : Failed to send email to user@example.com: ...
❌ ERROR EventConsumer : Error processing startup created event: ...
⚠️ WARN  UserServiceClientFallback : User-service is unavailable
```

### RabbitMQ not connecting

```
ERROR o.s.a.r.c.CachingConnectionFactory : Cannot connect to RabbitMQ
```

**Fix:** Ensure RabbitMQ is running:
```cmd
rabbitmq-server
```
Or check Windows Services for `RabbitMQ`.

---

## Quick Reference: Complete RabbitMQ Architecture

```
┌─────────────────┐     startup.created      ┌──────────────────┐
│ Startup Service  │ ──────────────────────►  │  startup.queue   │ ──► EventConsumer.handleStartupCreated()
│    (port 8083)   │     startup.deleted      │                  │     → Notify all investors + Send emails
└─────────────────┘ ──────────────────────►  │  inv.deleted.q   │ ──► (consumed by investment-service)
                                              │  team.deleted.q  │ ──► (consumed by team-service)
                                              └──────────────────┘

┌─────────────────┐     investment.created    ┌──────────────────┐
│Investment Service│ ──────────────────────►  │ investment.queue  │ ──► EventConsumer.handleInvestmentCreated()
│    (port 8084)   │                          │                  │     → Notify founder + Send email
└─────────────────┘                          └──────────────────┘

┌─────────────────┐     team.invite.sent      ┌──────────────────┐
│  Team Service    │ ──────────────────────►  │   team.queue     │ ──► EventConsumer.handleTeamInvite()
│    (port 8085)   │                          │                  │     → Notify invited user + Send email
└─────────────────┘                          └──────────────────┘

┌─────────────────┐     message.sent          ┌──────────────────┐
│Messaging Service │ ──────────────────────►  │ messaging.queue  │ ──► EventConsumer.handleMessageSent()
│    (port 8086)   │                          │                  │     → In-app notification only
└─────────────────┘                          └──────────────────┘

                    All use: founderlink.exchange (DirectExchange)
```
