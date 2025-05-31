-- V1__Complete_initial_schema.sql
-- Complete initial schema for Apartment Manager System
-- This schema exactly matches all JPA entity definitions

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'MANAGER', 'VIEWER'))
);

-- Create apartment_buildings table
CREATE TABLE IF NOT EXISTS apartment_buildings (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    default_monthly_fee DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create flats table
CREATE TABLE IF NOT EXISTS flats (
    id BIGSERIAL PRIMARY KEY,
    flat_number VARCHAR(255) NOT NULL,
    number_of_rooms INTEGER,
    area_sq_meters DECIMAL(10,2),
    tenant_name VARCHAR(100),
    tenant_contact VARCHAR(50),
    tenant_email VARCHAR(100),
    monthly_rent DECIMAL(10,2),
    security_deposit DECIMAL(10,2),
    tenant_move_in_date TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    apartment_building_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_flat_building FOREIGN KEY (apartment_building_id) REFERENCES apartment_buildings(id) ON DELETE CASCADE
);

-- Create payments table with all required columns
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    flat_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    notes TEXT,
    description TEXT,
    receipt_number VARCHAR(100),
    recorded_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_flat FOREIGN KEY (flat_id) REFERENCES flats(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_recorded_by FOREIGN KEY (recorded_by_user_id) REFERENCES users(id),
    CONSTRAINT payments_amount_positive CHECK (amount > 0),
    CONSTRAINT payments_method_check CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CREDIT_CARD', 'DEBIT_CARD', 'CHECK', 'ONLINE_PAYMENT', 'OTHER'))
);

-- Create monthly_dues table with all required columns
CREATE TABLE IF NOT EXISTS monthly_dues (
    id BIGSERIAL PRIMARY KEY,
    flat_id BIGINT NOT NULL,
    due_amount DECIMAL(10,2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    payment_id BIGINT,
    paid_date DATE,
    additional_charges_description TEXT,
    due_description TEXT,
    paid_amount DECIMAL(10,2) DEFAULT 0.00,
    payment_date TIMESTAMP,
    base_rent DECIMAL(10,2),
    additional_charges DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_due_flat FOREIGN KEY (flat_id) REFERENCES flats(id) ON DELETE CASCADE,
    CONSTRAINT fk_due_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT uk_flat_due_date UNIQUE (flat_id, due_date),
    CONSTRAINT monthly_dues_amount_positive CHECK (due_amount > 0),
    CONSTRAINT monthly_dues_status_check CHECK (status IN ('UNPAID', 'PAID', 'PARTIALLY_PAID', 'OVERDUE', 'WAIVED', 'CANCELLED'))
);

-- Create expenses table
CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL,
    expense_category VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    expense_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    notes TEXT,
    vendor_name VARCHAR(100),
    invoice_number VARCHAR(50),
    recorded_by_user_id BIGINT NOT NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_frequency VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_expense_building FOREIGN KEY (building_id) REFERENCES apartment_buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_recorded_by FOREIGN KEY (recorded_by_user_id) REFERENCES users(id),
    CONSTRAINT expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT expenses_category_check CHECK (expense_category IN ('MAINTENANCE', 'UTILITIES', 'CLEANING', 'SECURITY', 'INSURANCE', 'TAXES', 'MANAGEMENT', 'REPAIRS', 'LANDSCAPING', 'ELEVATOR', 'SUPPLIES', 'LEGAL', 'ACCOUNTING', 'MARKETING', 'OTHER')),
    CONSTRAINT expenses_recurrence_check CHECK (recurrence_frequency IS NULL OR recurrence_frequency IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL'))
);

-- Create user_building_assignments table
CREATE TABLE IF NOT EXISTS user_building_assignments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    assigned_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_date TIMESTAMP,
    assigned_by_user_id BIGINT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_user_assignment FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_building_assignment FOREIGN KEY (building_id) REFERENCES apartment_buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(id),
    CONSTRAINT uk_user_building UNIQUE (user_id, building_id)
);

-- Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    user_id BIGINT,
    username VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    description VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    correlation_id VARCHAR(36),
    result VARCHAR(10) NOT NULL,
    error_message VARCHAR(500),
    CONSTRAINT audit_logs_result_check CHECK (result IN ('SUCCESS', 'FAILURE'))
);

-- Create indexes for performance

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Flats table indexes
CREATE INDEX IF NOT EXISTS idx_flats_building_id ON flats(apartment_building_id);
CREATE INDEX IF NOT EXISTS idx_flats_is_active ON flats(is_active);

-- Payments table indexes
CREATE INDEX IF NOT EXISTS idx_payments_flat_id ON payments(flat_id);
CREATE INDEX IF NOT EXISTS idx_payments_date ON payments(payment_date);
CREATE INDEX IF NOT EXISTS idx_payments_recorded_by ON payments(recorded_by_user_id);

-- Monthly dues table indexes
CREATE INDEX IF NOT EXISTS idx_monthly_dues_flat_id ON monthly_dues(flat_id);
CREATE INDEX IF NOT EXISTS idx_monthly_dues_due_date ON monthly_dues(due_date);
CREATE INDEX IF NOT EXISTS idx_monthly_dues_status ON monthly_dues(status);
CREATE INDEX IF NOT EXISTS idx_monthly_dues_payment_id ON monthly_dues(payment_id);

-- Expenses table indexes
CREATE INDEX IF NOT EXISTS idx_expense_building_date ON expenses(building_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_expense_category ON expenses(expense_category);
CREATE INDEX IF NOT EXISTS idx_expense_recorded_by ON expenses(recorded_by_user_id);

-- User building assignments indexes
CREATE INDEX IF NOT EXISTS idx_user_building_user_id ON user_building_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_user_building_building_id ON user_building_assignments(building_id);
CREATE INDEX IF NOT EXISTS idx_user_building_is_active ON user_building_assignments(is_active);

-- Audit logs indexes
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_entity_type_id ON audit_logs(entity_type, entity_id);

-- Add comments for documentation
COMMENT ON TABLE users IS 'System users with different roles (ADMIN, MANAGER, VIEWER)';
COMMENT ON TABLE apartment_buildings IS 'Apartment buildings managed by the system';
COMMENT ON TABLE flats IS 'Individual flats/apartments within buildings';
COMMENT ON TABLE payments IS 'Payment records for flats with optimistic locking';
COMMENT ON TABLE monthly_dues IS 'Monthly dues/charges for each flat with idempotency';
COMMENT ON TABLE expenses IS 'Expenses incurred by apartment buildings';
COMMENT ON TABLE user_building_assignments IS 'Many-to-many relationship between users and buildings they manage';
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all system actions';

COMMENT ON COLUMN users.role IS 'User role: ADMIN can do everything, MANAGER can manage assigned buildings, VIEWER can only view';
COMMENT ON COLUMN flats.is_active IS 'Soft delete flag - false means the flat is logically deleted';
COMMENT ON COLUMN payments.version IS 'Version field for optimistic locking to prevent concurrent updates';
COMMENT ON COLUMN payments.payment_date IS 'Timestamp when payment was made (not just date)';
COMMENT ON COLUMN monthly_dues.status IS 'Payment status tracking including partial payments';
COMMENT ON COLUMN expenses.is_recurring IS 'Whether this is a recurring expense';
COMMENT ON COLUMN user_building_assignments.is_active IS 'Track active/inactive assignments without deleting history';