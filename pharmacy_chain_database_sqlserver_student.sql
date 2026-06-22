/*
    Pharmacy Chain Management System - SQL Server Database Script
    Version: Student-friendly / Lightweight

    Design notes:
    - Use INT IDENTITY instead of BIGINT for IDs.
    - Use INT for VND money values instead of DECIMAL/MONEY.
      Example: 25000 means 25,000 VND.
    - Use INT for quantities.
    - Use VARCHAR for status/enum fields.
    - Use NVARCHAR only where Vietnamese text is needed.
    - Tables are grouped by the 4 student flows.
*/

IF DB_ID(N'PharmacyChainDB_Student') IS NULL
BEGIN
    CREATE DATABASE PharmacyChainDB_Student;
END
GO

USE PharmacyChainDB_Student;
GO

/* =========================================================
   DROP TABLES FOR DEVELOPMENT RERUN
   ========================================================= */
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS accounting_periods;
DROP TABLE IF EXISTS expense_vouchers;
DROP TABLE IF EXISTS stock_transfer_details;
DROP TABLE IF EXISTS stock_transfers;

DROP TABLE IF EXISTS refund_requests;
DROP TABLE IF EXISTS cash_shifts;
DROP TABLE IF EXISTS payment_transactions;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS sale_details;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS prescription_details;
DROP TABLE IF EXISTS prescriptions;
DROP TABLE IF EXISTS customers;

DROP TABLE IF EXISTS stock_count_details;
DROP TABLE IF EXISTS stock_counts;
DROP TABLE IF EXISTS inventory_transactions;
DROP TABLE IF EXISTS inventory_batches;
DROP TABLE IF EXISTS goods_receipt_details;
DROP TABLE IF EXISTS goods_receipts;
DROP TABLE IF EXISTS purchase_request_details;
DROP TABLE IF EXISTS purchase_requests;
DROP TABLE IF EXISTS suppliers;

DROP TABLE IF EXISTS system_settings;
DROP TABLE IF EXISTS medicines;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS app_users;
DROP TABLE IF EXISTS branches;
DROP TABLE IF EXISTS roles;
GO

/* =========================================================
   FLOW 1 - AUTH, BRANCH, MEDICINE, SETTING
   Owner: Student 1
   ========================================================= */

CREATE TABLE roles (
    role_id INT IDENTITY(1,1) PRIMARY KEY,
    role_code VARCHAR(30) NOT NULL UNIQUE,
    role_name NVARCHAR(100) NOT NULL,
    description NVARCHAR(255) NULL,
    is_active BIT NOT NULL DEFAULT 1
);
GO

CREATE TABLE branches (
    branch_id INT IDENTITY(1,1) PRIMARY KEY,
    branch_code VARCHAR(30) NOT NULL UNIQUE,
    branch_name NVARCHAR(150) NOT NULL,
    address NVARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME NULL,
    CONSTRAINT CK_branches_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
GO

CREATE TABLE app_users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    role_id INT NOT NULL,
    branch_id INT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name NVARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(120) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME NULL,
    CONSTRAINT FK_app_users_roles FOREIGN KEY (role_id) REFERENCES roles(role_id),
    CONSTRAINT FK_app_users_branches FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT CK_app_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'INACTIVE'))
);
GO

CREATE TABLE categories (
    category_id INT IDENTITY(1,1) PRIMARY KEY,
    category_name NVARCHAR(120) NOT NULL,
    description NVARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT CK_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
GO

CREATE TABLE medicines (
    medicine_id INT IDENTITY(1,1) PRIMARY KEY,
    category_id INT NOT NULL,
    medicine_code VARCHAR(40) NOT NULL UNIQUE,
    barcode VARCHAR(60) NULL,
    medicine_name NVARCHAR(180) NOT NULL,
    active_ingredient NVARCHAR(150) NULL,
    strength NVARCHAR(80) NULL,
    dosage_form NVARCHAR(80) NULL,
    unit NVARCHAR(30) NOT NULL,
    manufacturer NVARCHAR(150) NULL,
    country_of_origin NVARCHAR(80) NULL,
    sale_price INT NOT NULL DEFAULT 0,
    min_stock_level INT NOT NULL DEFAULT 0,
    requires_prescription BIT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME NULL,
    CONSTRAINT FK_medicines_categories FOREIGN KEY (category_id) REFERENCES categories(category_id),
    CONSTRAINT CK_medicines_price CHECK (sale_price >= 0),
    CONSTRAINT CK_medicines_min_stock CHECK (min_stock_level >= 0),
    CONSTRAINT CK_medicines_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
GO

CREATE TABLE system_settings (
    setting_id INT IDENTITY(1,1) PRIMARY KEY,
    setting_key VARCHAR(80) NOT NULL UNIQUE,
    setting_value VARCHAR(100) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description NVARCHAR(255) NULL,
    updated_at DATETIME NULL,
    CONSTRAINT CK_system_settings_type CHECK (value_type IN ('STRING', 'INT', 'MONEY', 'BOOLEAN'))
);
GO

/* =========================================================
   FLOW 2 - SUPPLIER, PURCHASE, GOODS RECEIPT, INVENTORY
   Owner: Student 2
   ========================================================= */

CREATE TABLE suppliers (
    supplier_id INT IDENTITY(1,1) PRIMARY KEY,
    supplier_code VARCHAR(40) NOT NULL UNIQUE,
    supplier_name NVARCHAR(180) NOT NULL,
    supplier_type VARCHAR(40) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(120) NULL,
    address NVARCHAR(255) NULL,
    tax_code VARCHAR(30) NULL,
    contact_person NVARCHAR(120) NULL,
    license_no VARCHAR(80) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME NULL,
    CONSTRAINT CK_suppliers_type CHECK (supplier_type IN ('MANUFACTURER', 'DISTRIBUTOR', 'IMPORTER', 'WHOLESALER')),
    CONSTRAINT CK_suppliers_status CHECK (status IN ('DRAFT', 'APPROVED', 'SUSPENDED', 'INACTIVE'))
);
GO

CREATE TABLE purchase_requests (
    purchase_request_id INT IDENTITY(1,1) PRIMARY KEY,
    request_code VARCHAR(40) NOT NULL UNIQUE,
    branch_id INT NOT NULL,
    supplier_id INT NOT NULL,
    requested_by INT NOT NULL,
    approved_by INT NULL,
    request_date DATETIME NOT NULL DEFAULT GETDATE(),
    approved_at DATETIME NULL,
    expected_delivery_date DATE NULL,
    total_estimated_amount INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    note NVARCHAR(500) NULL,
    reject_reason NVARCHAR(500) NULL,
    CONSTRAINT FK_purchase_requests_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_purchase_requests_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    CONSTRAINT FK_purchase_requests_requested_by FOREIGN KEY (requested_by) REFERENCES app_users(user_id),
    CONSTRAINT FK_purchase_requests_approved_by FOREIGN KEY (approved_by) REFERENCES app_users(user_id),
    CONSTRAINT CK_purchase_requests_amount CHECK (total_estimated_amount >= 0),
    CONSTRAINT CK_purchase_requests_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'APPROVED', 'PARTIALLY_APPROVED', 'REJECTED', 'CANCELLED', 'RECEIVING', 'COMPLETED'
    ))
);
GO

CREATE TABLE purchase_request_details (
    purchase_request_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    purchase_request_id INT NOT NULL,
    medicine_id INT NOT NULL,
    requested_quantity INT NOT NULL,
    approved_quantity INT NULL,
    expected_unit_price INT NOT NULL DEFAULT 0,
    note NVARCHAR(255) NULL,
    CONSTRAINT FK_pr_details_request FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests(purchase_request_id),
    CONSTRAINT FK_pr_details_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    CONSTRAINT CK_pr_details_quantity CHECK (requested_quantity > 0),
    CONSTRAINT CK_pr_details_approved_quantity CHECK (approved_quantity IS NULL OR approved_quantity >= 0),
    CONSTRAINT CK_pr_details_price CHECK (expected_unit_price >= 0)
);
GO

CREATE TABLE goods_receipts (
    goods_receipt_id INT IDENTITY(1,1) PRIMARY KEY,
    receipt_code VARCHAR(40) NOT NULL UNIQUE,
    purchase_request_id INT NOT NULL,
    branch_id INT NOT NULL,
    supplier_id INT NOT NULL,
    received_by INT NOT NULL,
    approved_by INT NULL,
    received_at DATETIME NOT NULL DEFAULT GETDATE(),
    posted_at DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_actual_amount INT NOT NULL DEFAULT 0,
    has_variance BIT NOT NULL DEFAULT 0,
    note NVARCHAR(500) NULL,
    CONSTRAINT FK_goods_receipts_pr FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests(purchase_request_id),
    CONSTRAINT FK_goods_receipts_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_goods_receipts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    CONSTRAINT FK_goods_receipts_received_by FOREIGN KEY (received_by) REFERENCES app_users(user_id),
    CONSTRAINT FK_goods_receipts_approved_by FOREIGN KEY (approved_by) REFERENCES app_users(user_id),
    CONSTRAINT CK_goods_receipts_amount CHECK (total_actual_amount >= 0),
    CONSTRAINT CK_goods_receipts_status CHECK (status IN (
        'DRAFT', 'PENDING_ADMIN_APPROVAL', 'APPROVED', 'POSTED', 'REJECTED', 'CANCELLED'
    ))
);
GO

CREATE TABLE goods_receipt_details (
    goods_receipt_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    goods_receipt_id INT NOT NULL,
    medicine_id INT NOT NULL,
    batch_number VARCHAR(60) NOT NULL,
    expiry_date DATE NOT NULL,
    ordered_quantity INT NOT NULL DEFAULT 0,
    received_quantity INT NOT NULL,
    accepted_quantity INT NOT NULL DEFAULT 0,
    rejected_quantity INT NOT NULL DEFAULT 0,
    actual_unit_price INT NOT NULL DEFAULT 0,
    inspection_result VARCHAR(30) NOT NULL DEFAULT 'PASS',
    rejection_reason NVARCHAR(255) NULL,
    CONSTRAINT FK_gr_details_receipt FOREIGN KEY (goods_receipt_id) REFERENCES goods_receipts(goods_receipt_id),
    CONSTRAINT FK_gr_details_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    CONSTRAINT CK_gr_details_ordered_quantity CHECK (ordered_quantity >= 0),
    CONSTRAINT CK_gr_details_received_quantity CHECK (received_quantity >= 0),
    CONSTRAINT CK_gr_details_accepted_quantity CHECK (accepted_quantity >= 0),
    CONSTRAINT CK_gr_details_rejected_quantity CHECK (rejected_quantity >= 0),
    CONSTRAINT CK_gr_details_actual_price CHECK (actual_unit_price >= 0),
    CONSTRAINT CK_gr_details_inspection CHECK (inspection_result IN ('PASS', 'FAIL', 'PENDING'))
);
GO

CREATE TABLE inventory_batches (
    inventory_batch_id INT IDENTITY(1,1) PRIMARY KEY,
    branch_id INT NOT NULL,
    medicine_id INT NOT NULL,
    supplier_id INT NULL,
    batch_number VARCHAR(60) NOT NULL,
    expiry_date DATE NOT NULL,
    quantity_on_hand INT NOT NULL DEFAULT 0,
    unit_cost INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    received_date DATETIME NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME NULL,
    CONSTRAINT FK_inventory_batches_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_inventory_batches_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    CONSTRAINT FK_inventory_batches_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    CONSTRAINT CK_inventory_batches_quantity CHECK (quantity_on_hand >= 0),
    CONSTRAINT CK_inventory_batches_cost CHECK (unit_cost >= 0),
    CONSTRAINT CK_inventory_batches_status CHECK (status IN ('AVAILABLE', 'EXPIRED', 'DAMAGED', 'RECALLED', 'DISPOSED')),
    CONSTRAINT UQ_inventory_batch UNIQUE (branch_id, medicine_id, batch_number, expiry_date)
);
GO

CREATE TABLE inventory_transactions (
    inventory_transaction_id INT IDENTITY(1,1) PRIMARY KEY,
    branch_id INT NOT NULL,
    medicine_id INT NOT NULL,
    inventory_batch_id INT NULL,
    transaction_type VARCHAR(40) NOT NULL,
    quantity INT NOT NULL,
    unit_cost INT NULL,
    reference_type VARCHAR(40) NULL,
    reference_id INT NULL,
    created_by INT NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    note NVARCHAR(255) NULL,
    CONSTRAINT FK_inventory_transactions_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_inventory_transactions_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    CONSTRAINT FK_inventory_transactions_batch FOREIGN KEY (inventory_batch_id) REFERENCES inventory_batches(inventory_batch_id),
    CONSTRAINT FK_inventory_transactions_user FOREIGN KEY (created_by) REFERENCES app_users(user_id),
    CONSTRAINT CK_inventory_transactions_quantity CHECK (quantity > 0),
    CONSTRAINT CK_inventory_transactions_type CHECK (transaction_type IN (
        'RECEIPT', 'SALE', 'SALE_RETURN', 'TRANSFER_OUT', 'TRANSFER_IN',
        'ADJUSTMENT_IN', 'ADJUSTMENT_OUT', 'EXPIRED_DISPOSAL'
    ))
);
GO

CREATE TABLE stock_counts (
    stock_count_id INT IDENTITY(1,1) PRIMARY KEY,
    count_code VARCHAR(40) NOT NULL UNIQUE,
    branch_id INT NOT NULL,
    created_by INT NOT NULL,
    approved_by INT NULL,
    count_date DATETIME NOT NULL DEFAULT GETDATE(),
    approved_at DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_variance_amount INT NOT NULL DEFAULT 0,
    note NVARCHAR(500) NULL,
    CONSTRAINT FK_stock_counts_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_stock_counts_created_by FOREIGN KEY (created_by) REFERENCES app_users(user_id),
    CONSTRAINT FK_stock_counts_approved_by FOREIGN KEY (approved_by) REFERENCES app_users(user_id),
    CONSTRAINT CK_stock_counts_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'POSTED', 'CANCELLED'))
);
GO

CREATE TABLE stock_count_details (
    stock_count_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    stock_count_id INT NOT NULL,
    inventory_batch_id INT NOT NULL,
    system_quantity INT NOT NULL,
    actual_quantity INT NOT NULL,
    variance_quantity AS (actual_quantity - system_quantity),
    reason NVARCHAR(255) NULL,
    CONSTRAINT FK_stock_count_details_count FOREIGN KEY (stock_count_id) REFERENCES stock_counts(stock_count_id),
    CONSTRAINT FK_stock_count_details_batch FOREIGN KEY (inventory_batch_id) REFERENCES inventory_batches(inventory_batch_id),
    CONSTRAINT CK_stock_count_details_system_quantity CHECK (system_quantity >= 0),
    CONSTRAINT CK_stock_count_details_actual_quantity CHECK (actual_quantity >= 0)
);
GO

/* =========================================================
   FLOW 3 - CUSTOMER, PRESCRIPTION, SALE, PAYMENT, CASH SHIFT
   Owner: Student 3
   ========================================================= */

CREATE TABLE customers (
    customer_id INT IDENTITY(1,1) PRIMARY KEY,
    full_name NVARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,
    date_of_birth DATE NULL,
    gender VARCHAR(10) NULL,
    address NVARCHAR(255) NULL,
    allergy_note NVARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CK_customers_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'))
);
GO

CREATE TABLE prescriptions (
    prescription_id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id INT NOT NULL,
    prescription_code VARCHAR(50) NULL,
    doctor_name NVARCHAR(120) NULL,
    clinic_name NVARCHAR(180) NULL,
    prescription_date DATE NULL,
    valid_until DATE NULL,
    note NVARCHAR(500) NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_prescriptions_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT FK_prescriptions_created_by FOREIGN KEY (created_by) REFERENCES app_users(user_id)
);
GO

CREATE TABLE prescription_details (
    prescription_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    prescription_id INT NOT NULL,
    medicine_id INT NOT NULL,
    prescribed_quantity INT NOT NULL,
    dosage_instruction NVARCHAR(255) NULL,
    CONSTRAINT FK_prescription_details_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id),
    CONSTRAINT FK_prescription_details_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    CONSTRAINT CK_prescription_details_quantity CHECK (prescribed_quantity > 0)
);
GO

CREATE TABLE sales (
    sale_id INT IDENTITY(1,1) PRIMARY KEY,
    sale_code VARCHAR(40) NOT NULL UNIQUE,
    branch_id INT NOT NULL,
    customer_id INT NULL,
    prescription_id INT NULL,
    pharmacist_id INT NOT NULL,
    sale_date DATETIME NOT NULL DEFAULT GETDATE(),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_amount INT NOT NULL DEFAULT 0,
    discount_amount INT NOT NULL DEFAULT 0,
    final_amount INT NOT NULL DEFAULT 0,
    note NVARCHAR(500) NULL,
    CONSTRAINT FK_sales_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT FK_sales_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id),
    CONSTRAINT FK_sales_pharmacist FOREIGN KEY (pharmacist_id) REFERENCES app_users(user_id),
    CONSTRAINT CK_sales_amounts CHECK (total_amount >= 0 AND discount_amount >= 0 AND final_amount >= 0),
    CONSTRAINT CK_sales_status CHECK (status IN ('DRAFT', 'COMPLETED', 'VOIDED', 'REFUNDED'))
);
GO

CREATE TABLE sale_details (
    sale_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    sale_id INT NOT NULL,
    medicine_id INT NOT NULL,
    inventory_batch_id INT NULL,
    quantity INT NOT NULL,
    unit_price INT NOT NULL,
    line_amount INT NOT NULL,
    CONSTRAINT FK_sale_details_sale FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
    CONSTRAINT FK_sale_details_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    CONSTRAINT FK_sale_details_batch FOREIGN KEY (inventory_batch_id) REFERENCES inventory_batches(inventory_batch_id),
    CONSTRAINT CK_sale_details_quantity CHECK (quantity > 0),
    CONSTRAINT CK_sale_details_prices CHECK (unit_price >= 0 AND line_amount >= 0)
);
GO

CREATE TABLE payments (
    payment_id INT IDENTITY(1,1) PRIMARY KEY,
    sale_id INT NOT NULL,
    payment_code VARCHAR(40) NOT NULL UNIQUE,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    amount INT NOT NULL,
    customer_paid_amount INT NULL,
    change_amount INT NULL,
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    note NVARCHAR(255) NULL,
    CONSTRAINT FK_payments_sale FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
    CONSTRAINT CK_payments_amount CHECK (amount >= 0),
    CONSTRAINT CK_payments_method CHECK (payment_method IN ('CASH', 'ONLINE')),
    CONSTRAINT CK_payments_status CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'REFUNDED'))
);
GO

CREATE TABLE payment_transactions (
    payment_transaction_id INT IDENTITY(1,1) PRIMARY KEY,
    payment_id INT NOT NULL,
    gateway_transaction_code VARCHAR(100) NULL,
    transaction_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    amount INT NOT NULL,
    raw_message NVARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_payment_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id),
    CONSTRAINT CK_payment_transactions_amount CHECK (amount >= 0),
    CONSTRAINT CK_payment_transactions_status CHECK (transaction_status IN ('PENDING', 'SUCCESS', 'FAILED'))
);
GO

CREATE TABLE cash_shifts (
    cash_shift_id INT IDENTITY(1,1) PRIMARY KEY,
    branch_id INT NOT NULL,
    pharmacist_id INT NOT NULL,
    opened_at DATETIME NOT NULL DEFAULT GETDATE(),
    closed_at DATETIME NULL,
    opening_cash_amount INT NOT NULL DEFAULT 0,
    system_cash_amount INT NOT NULL DEFAULT 0,
    actual_cash_amount INT NULL,
    difference_amount INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    manager_confirmed_by INT NULL,
    manager_confirmed_at DATETIME NULL,
    note NVARCHAR(500) NULL,
    CONSTRAINT FK_cash_shifts_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_cash_shifts_pharmacist FOREIGN KEY (pharmacist_id) REFERENCES app_users(user_id),
    CONSTRAINT FK_cash_shifts_manager FOREIGN KEY (manager_confirmed_by) REFERENCES app_users(user_id),
    CONSTRAINT CK_cash_shifts_amounts CHECK (opening_cash_amount >= 0 AND system_cash_amount >= 0),
    CONSTRAINT CK_cash_shifts_status CHECK (status IN ('OPEN', 'CLOSED', 'PENDING_ADMIN_REVIEW', 'CONFIRMED'))
);
GO

CREATE TABLE refund_requests (
    refund_request_id INT IDENTITY(1,1) PRIMARY KEY,
    sale_id INT NOT NULL,
    requested_by INT NOT NULL,
    approved_by INT NULL,
    requested_at DATETIME NOT NULL DEFAULT GETDATE(),
    approved_at DATETIME NULL,
    refund_amount INT NOT NULL DEFAULT 0,
    reason NVARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT FK_refund_requests_sale FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
    CONSTRAINT FK_refund_requests_requested_by FOREIGN KEY (requested_by) REFERENCES app_users(user_id),
    CONSTRAINT FK_refund_requests_approved_by FOREIGN KEY (approved_by) REFERENCES app_users(user_id),
    CONSTRAINT CK_refund_requests_amount CHECK (refund_amount >= 0),
    CONSTRAINT CK_refund_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED'))
);
GO

/* =========================================================
   FLOW 4 - TRANSFER, EXPENSE, DASHBOARD, AUDIT, PERIOD
   Owner: Student 4
   ========================================================= */

CREATE TABLE stock_transfers (
    stock_transfer_id INT IDENTITY(1,1) PRIMARY KEY,
    transfer_code VARCHAR(40) NOT NULL UNIQUE,
    from_branch_id INT NOT NULL,
    to_branch_id INT NOT NULL,
    requested_by INT NOT NULL,
    approved_by INT NULL,
    sent_by INT NULL,
    received_by INT NULL,
    requested_at DATETIME NOT NULL DEFAULT GETDATE(),
    approved_at DATETIME NULL,
    sent_at DATETIME NULL,
    received_at DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_value_amount INT NOT NULL DEFAULT 0,
    note NVARCHAR(500) NULL,
    CONSTRAINT FK_stock_transfers_from_branch FOREIGN KEY (from_branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_stock_transfers_to_branch FOREIGN KEY (to_branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_stock_transfers_requested_by FOREIGN KEY (requested_by) REFERENCES app_users(user_id),
    CONSTRAINT FK_stock_transfers_approved_by FOREIGN KEY (approved_by) REFERENCES app_users(user_id),
    CONSTRAINT FK_stock_transfers_sent_by FOREIGN KEY (sent_by) REFERENCES app_users(user_id),
    CONSTRAINT FK_stock_transfers_received_by FOREIGN KEY (received_by) REFERENCES app_users(user_id),
    CONSTRAINT CK_stock_transfers_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'PENDING_ADMIN_APPROVAL', 'APPROVED', 'REJECTED', 'IN_TRANSIT', 'RECEIVED', 'CANCELLED'
    )),
    CONSTRAINT CK_stock_transfers_different_branch CHECK (from_branch_id <> to_branch_id),
    CONSTRAINT CK_stock_transfers_value CHECK (total_value_amount >= 0)
);
GO

CREATE TABLE stock_transfer_details (
    stock_transfer_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    stock_transfer_id INT NOT NULL,
    medicine_id INT NOT NULL,
    from_inventory_batch_id INT NULL,
    requested_quantity INT NOT NULL,
    sent_quantity INT NULL,
    received_quantity INT NULL,
    CONSTRAINT FK_st_details_transfer FOREIGN KEY (stock_transfer_id) REFERENCES stock_transfers(stock_transfer_id),
    CONSTRAINT FK_st_details_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id),
    CONSTRAINT FK_st_details_batch FOREIGN KEY (from_inventory_batch_id) REFERENCES inventory_batches(inventory_batch_id),
    CONSTRAINT CK_st_details_requested_quantity CHECK (requested_quantity > 0),
    CONSTRAINT CK_st_details_sent_quantity CHECK (sent_quantity IS NULL OR sent_quantity >= 0),
    CONSTRAINT CK_st_details_received_quantity CHECK (received_quantity IS NULL OR received_quantity >= 0)
);
GO

CREATE TABLE expense_vouchers (
    expense_voucher_id INT IDENTITY(1,1) PRIMARY KEY,
    expense_code VARCHAR(40) NOT NULL UNIQUE,
    branch_id INT NOT NULL,
    created_by INT NOT NULL,
    approved_by INT NULL,
    expense_type VARCHAR(40) NOT NULL,
    amount INT NOT NULL,
    expense_date DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    document_no VARCHAR(80) NULL,
    description NVARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    approved_at DATETIME NULL,
    CONSTRAINT FK_expense_vouchers_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id),
    CONSTRAINT FK_expense_vouchers_created_by FOREIGN KEY (created_by) REFERENCES app_users(user_id),
    CONSTRAINT FK_expense_vouchers_approved_by FOREIGN KEY (approved_by) REFERENCES app_users(user_id),
    CONSTRAINT CK_expense_vouchers_amount CHECK (amount >= 0),
    CONSTRAINT CK_expense_vouchers_type CHECK (expense_type IN ('RENT', 'ELECTRICITY', 'WATER', 'SALARY', 'DELIVERY', 'OTHER')),
    CONSTRAINT CK_expense_vouchers_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'PAID', 'CANCELLED'))
);
GO

CREATE TABLE accounting_periods (
    accounting_period_id INT IDENTITY(1,1) PRIMARY KEY,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    locked_by INT NULL,
    locked_at DATETIME NULL,
    unlock_reason NVARCHAR(500) NULL,
    CONSTRAINT FK_accounting_periods_locked_by FOREIGN KEY (locked_by) REFERENCES app_users(user_id),
    CONSTRAINT UQ_accounting_period UNIQUE (period_year, period_month),
    CONSTRAINT CK_accounting_periods_month CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT CK_accounting_periods_status CHECK (status IN ('OPEN', 'LOCKED'))
);
GO

CREATE TABLE notifications (
    notification_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    branch_id INT NULL,
    title NVARCHAR(150) NOT NULL,
    message NVARCHAR(500) NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_notifications_user FOREIGN KEY (user_id) REFERENCES app_users(user_id),
    CONSTRAINT FK_notifications_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);
GO

CREATE TABLE audit_logs (
    audit_log_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    branch_id INT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id INT NULL,
    old_value NVARCHAR(1000) NULL,
    new_value NVARCHAR(1000) NULL,
    reason NVARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_audit_logs_user FOREIGN KEY (user_id) REFERENCES app_users(user_id),
    CONSTRAINT FK_audit_logs_branch FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);
GO

/* =========================================================
   INDEXES - LIGHTWEIGHT BUT USEFUL FOR STUDENT PROJECT
   ========================================================= */

CREATE INDEX IX_app_users_role_branch ON app_users(role_id, branch_id);
CREATE INDEX IX_medicines_name ON medicines(medicine_name);
CREATE INDEX IX_medicines_barcode ON medicines(barcode);

CREATE INDEX IX_purchase_requests_branch_status ON purchase_requests(branch_id, status);
CREATE INDEX IX_goods_receipts_pr_status ON goods_receipts(purchase_request_id, status);

CREATE INDEX IX_inventory_batches_branch_medicine ON inventory_batches(branch_id, medicine_id);
CREATE INDEX IX_inventory_batches_expiry ON inventory_batches(expiry_date);
CREATE INDEX IX_inventory_transactions_branch_date ON inventory_transactions(branch_id, created_at);
CREATE INDEX IX_inventory_transactions_medicine ON inventory_transactions(medicine_id);

CREATE INDEX IX_sales_branch_date ON sales(branch_id, sale_date);
CREATE INDEX IX_sales_status ON sales(status);
CREATE INDEX IX_payments_sale_status ON payments(sale_id, status);
CREATE INDEX IX_cash_shifts_branch_status ON cash_shifts(branch_id, status);

CREATE INDEX IX_stock_transfers_branch_status ON stock_transfers(from_branch_id, to_branch_id, status);
CREATE INDEX IX_expense_vouchers_branch_date ON expense_vouchers(branch_id, expense_date);
CREATE INDEX IX_audit_logs_target ON audit_logs(target_type, target_id);
GO

/* =========================================================
   SEED DATA
   ========================================================= */

INSERT INTO roles (role_code, role_name, description)
VALUES
('ADMIN', N'Admin', N'Quản trị toàn chuỗi'),
('BRANCH_MANAGER', N'Branch Manager', N'Quản lý chi nhánh'),
('PHARMACIST', N'Pharmacist', N'Dược sĩ bán hàng');
GO

INSERT INTO branches (branch_code, branch_name, address, phone)
VALUES
('CN001', N'Chi nhánh trung tâm', N'Hà Nội', '0900000000');
GO

INSERT INTO categories (category_name, description)
VALUES
(N'Thuốc giảm đau', N'Nhóm thuốc giảm đau, hạ sốt'),
(N'Kháng sinh', N'Nhóm thuốc kê đơn'),
(N'Vitamin', N'Thực phẩm bổ sung/vitamin');
GO

INSERT INTO medicines (
    category_id, medicine_code, barcode, medicine_name, active_ingredient,
    strength, dosage_form, unit, manufacturer, country_of_origin,
    sale_price, min_stock_level, requires_prescription, status
)
VALUES
(1, 'MED001', '893000000001', N'Paracetamol 500mg', N'Paracetamol', N'500mg', N'Viên nén', N'viên', N'Công ty Dược A', N'Việt Nam', 2000, 50, 0, 'ACTIVE'),
(2, 'MED002', '893000000002', N'Amoxicillin 500mg', N'Amoxicillin', N'500mg', N'Viên nang', N'viên', N'Công ty Dược B', N'Việt Nam', 5000, 30, 1, 'ACTIVE'),
(3, 'MED003', '893000000003', N'Vitamin C 500mg', N'Vitamin C', N'500mg', N'Viên sủi', N'viên', N'Công ty Dược C', N'Việt Nam', 3000, 40, 0, 'ACTIVE');
GO

INSERT INTO system_settings (setting_key, setting_value, value_type, description)
VALUES
('PURCHASE_APPROVAL_LIMIT', '5000000', 'MONEY', N'Yêu cầu nhập hàng vượt số tiền này cần Admin duyệt'),
('STOCK_VARIANCE_LIMIT', '500000', 'MONEY', N'Chênh lệch kiểm kê vượt số tiền này cần Admin duyệt'),
('TRANSFER_APPROVAL_LIMIT', '3000000', 'MONEY', N'Điều chuyển vượt số tiền này cần Admin duyệt'),
('EXPENSE_APPROVAL_LIMIT', '1000000', 'MONEY', N'Phiếu chi vượt số tiền này cần Admin duyệt'),
('REFUND_APPROVAL_LIMIT', '500000', 'MONEY', N'Hoàn tiền vượt số tiền này cần Admin duyệt'),
('EXPIRY_WARNING_DAYS', '60', 'INT', N'Cảnh báo thuốc sắp hết hạn trước số ngày này');
GO

/*
    Optional test users.
    Replace password_hash with BCrypt hash when using Spring Security.
    For quick classroom demo only, you may temporarily store plain text if your login code has not added BCrypt yet.
*/
INSERT INTO app_users (role_id, branch_id, username, password_hash, full_name, phone, email)
VALUES
(1, NULL, 'admin', '123456', N'Quản trị hệ thống', '0900000001', 'admin@example.com'),
(2, 1, 'manager1', '123456', N'Quản lý chi nhánh 1', '0900000002', 'manager1@example.com'),
(3, 1, 'pharmacist1', '123456', N'Dược sĩ 1', '0900000003', 'pharmacist1@example.com');
GO

PRINT 'PharmacyChainDB_Student created successfully with lightweight INT-based schema.';
GO
