-- =====================================================
-- Wallet Service Database Schema
-- Database: wallet_db
-- Created: 2026-03-24
-- =====================================================

CREATE DATABASE IF NOT EXISTS wallet_db;
USE wallet_db;

-- =====================================================
-- WALLETS Table
-- Tracks startup fund balances
-- =====================================================
CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- Startup Reference
    startup_id BIGINT NOT NULL UNIQUE,
    
    -- Balance
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_startup_id (startup_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    reference_id BIGINT NOT NULL,
    source_payment_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_reference_id (reference_id),
    INDEX idx_wallet_id (wallet_id),
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Sample Data (for testing)
-- =====================================================
-- (Insert test data as needed)
