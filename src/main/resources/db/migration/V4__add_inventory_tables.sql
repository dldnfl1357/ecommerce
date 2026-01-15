-- Inventory 도메인 테이블 생성
-- 재고 관리, 재고 변경 이력

-- Inventories 테이블 (재고 정보)
CREATE TABLE inventories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_option_id BIGINT NOT NULL UNIQUE COMMENT '상품 옵션 ID',
    quantity INT NOT NULL DEFAULT 0 COMMENT '현재 재고 수량',
    reserved_quantity INT NOT NULL DEFAULT 0 COMMENT '예약된 재고 수량',
    safety_stock INT NOT NULL DEFAULT 10 COMMENT '안전 재고 수량',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (product_option_id) REFERENCES product_options(id) ON DELETE CASCADE,
    INDEX idx_inventory_product_option (product_option_id),
    INDEX idx_inventory_quantity (quantity),
    INDEX idx_inventory_available (quantity, reserved_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재고 정보';

-- Inventory Histories 테이블 (재고 변경 이력)
CREATE TABLE inventory_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id BIGINT NOT NULL COMMENT '재고 ID',
    product_option_id BIGINT NOT NULL COMMENT '상품 옵션 ID',
    change_type VARCHAR(20) NOT NULL COMMENT '변경 유형 (INCREASE, DECREASE, RESERVE, RELEASE, ADJUST, RETURN)',
    change_quantity INT NOT NULL COMMENT '변경 수량',
    before_quantity INT NOT NULL COMMENT '변경 전 수량',
    after_quantity INT NOT NULL COMMENT '변경 후 수량',
    reason VARCHAR(200) NULL COMMENT '변경 사유',
    reference_id BIGINT NULL COMMENT '참조 ID (주문 ID 등)',
    reference_type VARCHAR(50) NULL COMMENT '참조 유형 (ORDER 등)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (inventory_id) REFERENCES inventories(id) ON DELETE CASCADE,
    INDEX idx_history_inventory (inventory_id),
    INDEX idx_history_product_option (product_option_id),
    INDEX idx_history_change_type (change_type),
    INDEX idx_history_reference (reference_id, reference_type),
    INDEX idx_history_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='재고 변경 이력';

-- 기존 product_options에서 inventories로 초기 데이터 마이그레이션
-- (stock_quantity 컬럼이 있는 경우)
INSERT INTO inventories (product_option_id, quantity, reserved_quantity, safety_stock)
SELECT id, stock_quantity, 0, 10
FROM product_options
WHERE NOT EXISTS (SELECT 1 FROM inventories WHERE inventories.product_option_id = product_options.id);
