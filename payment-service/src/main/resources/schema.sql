-- =====================================================
-- Payment Service Database Schema
-- Database: payment_db
-- Created: 2026-03-24
-- =====================================================

CREATE DATABASE IF NOT EXISTS payment_db;
USE payment_db;

-- =====================================================
-- PAYMENTS Table
-- Tracks investment payment lifecycle
-- =====================================================
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Investment Reference
    investment_id BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,
    startup_id BIGINT NOT NULL,
    founder_id BIGINT NOT NULL,
    
    -- Amount and Status
    amount DECIMAL(19, 2) NOT NULL,
    status ENUM('PENDING_HOLD', 'HELD', 'CAPTURED', 'TRANSFERRED', 'RELEASED', 'FAILED') 
        NOT NULL DEFAULT 'PENDING_HOLD',
    
    -- Idempotency (prevents double-charges on retries)
    idempotency_key VARCHAR(36) NOT NULL UNIQUE,
    
    -- External Gateway References
    external_payment_id VARCHAR(100),
    failure_reason TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_investment_id (investment_id),
    INDEX idx_investor_id (investor_id),
    INDEX idx_startup_id (startup_id),
    INDEX idx_status (status),
    INDEX idx_idempotency_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- PAYMENT_TRANSACTION_LOGS Table
-- Audit trail for all payment operations
-- =====================================================
CREATE TABLE IF NOT EXISTS payment_transaction_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Payment Reference
    payment_id BIGINT NOT NULL,
    
    -- Operation Details
    action VARCHAR(50) NOT NULL,  -- HOLD_SUCCESS, CAPTURE_SUCCESS, RELEASE_SUCCESS, etc.
    details JSON,                  -- Error messages, gateway responses
    
    -- Timestamp
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_payment_id (payment_id),
    INDEX idx_action (action),
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Sample Data (for testing)
-- =====================================================
-- (Insert test data as needed)
