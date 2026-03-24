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

-- =====================================================
-- Sample Data (for testing)
-- =====================================================
-- (Insert test data as needed)
