-- Orders Table
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    address_id BIGINT NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    delivery_fee DECIMAL(15, 2) DEFAULT 0,
    final_amount DECIMAL(15, 2) NOT NULL,
    point_used INT DEFAULT 0,
    point_earned INT DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ordered_at DATETIME NOT NULL,
    paid_at DATETIME,
    shipped_at DATETIME,
    delivered_at DATETIME,
    cancelled_at DATETIME,
    cancel_reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_member (member_id),
    INDEX idx_order_number (order_number),
    INDEX idx_order_status (status),
    INDEX idx_order_ordered_at (ordered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Order Items Table
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    option_name VARCHAR(200),
    quantity INT NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    discount_rate INT DEFAULT 0,
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    final_price DECIMAL(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    seller_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_item_order (order_id),
    INDEX idx_order_item_seller (seller_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Carts Table
CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cart_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cart Items Table
CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    option_name VARCHAR(200),
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(15, 2) NOT NULL,
    discount_rate INT DEFAULT 0,
    seller_id BIGINT,
    is_selected BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cart_item_cart (cart_id),
    UNIQUE INDEX idx_cart_item_unique (cart_id, product_option_id),
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payments Table
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    payment_key VARCHAR(100),
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(15, 2) NOT NULL,
    paid_amount DECIMAL(15, 2) DEFAULT 0,
    refunded_amount DECIMAL(15, 2) DEFAULT 0,
    card_company VARCHAR(50),
    card_number VARCHAR(20),
    installment_months INT DEFAULT 0,
    bank_name VARCHAR(50),
    account_number VARCHAR(50),
    virtual_account_holder VARCHAR(50),
    virtual_account_due_date DATETIME,
    paid_at DATETIME,
    cancelled_at DATETIME,
    refunded_at DATETIME,
    failure_reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_order (order_id),
    INDEX idx_payment_key (payment_key),
    INDEX idx_payment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Deliveries Table
CREATE TABLE deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    type VARCHAR(30) NOT NULL,
    carrier_code VARCHAR(20),
    carrier_name VARCHAR(50),
    tracking_number VARCHAR(50),
    recipient_name VARCHAR(50) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    address VARCHAR(200) NOT NULL,
    address_detail VARCHAR(200),
    delivery_request VARCHAR(500),
    estimated_delivery_date DATETIME,
    shipped_at DATETIME,
    delivered_at DATETIME,
    returned_at DATETIME,
    return_reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_delivery_order (order_id),
    INDEX idx_delivery_tracking (tracking_number),
    INDEX idx_delivery_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Delivery Histories Table
CREATE TABLE delivery_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    location VARCHAR(100),
    description VARCHAR(500),
    occurred_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_delivery_history_delivery (delivery_id),
    FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
