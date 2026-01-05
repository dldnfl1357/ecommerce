-- Product 도메인 테이블 생성
-- 카테고리, 상품, 상품옵션, 상품이미지

-- Categories 테이블 (계층형 구조)
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '카테고리명',
    parent_id BIGINT NULL COMMENT '상위 카테고리 ID',
    depth INT NOT NULL DEFAULT 0 COMMENT '깊이 (0: 대분류, 1: 중분류, 2: 소분류)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_category_parent (parent_id),
    INDEX idx_category_depth (depth),
    INDEX idx_category_active (is_active),
    INDEX idx_category_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품 카테고리';

-- Products 테이블
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL COMMENT '판매자 ID',
    category_id BIGINT NOT NULL COMMENT '카테고리 ID',
    name VARCHAR(200) NOT NULL COMMENT '상품명',
    description TEXT COMMENT '상품 설명',
    base_price INT NOT NULL COMMENT '기본 가격',
    discount_rate INT NOT NULL DEFAULT 0 COMMENT '할인율 (0-100)',
    discount_price INT NOT NULL DEFAULT 0 COMMENT '할인가',
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '상품 상태 (ON_SALE, STOP_SALE, SOLD_OUT, DELETED)',
    delivery_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '배송 유형 (ROCKET, ROCKET_FRESH, DAWN, NORMAL)',
    delivery_fee INT NOT NULL DEFAULT 3000 COMMENT '배송비',
    free_delivery_threshold INT NOT NULL DEFAULT 30000 COMMENT '무료배송 기준금액',
    average_rating DECIMAL(2,1) NOT NULL DEFAULT 0.0 COMMENT '평균 평점',
    review_count INT NOT NULL DEFAULT 0 COMMENT '리뷰 수',
    sales_count INT NOT NULL DEFAULT 0 COMMENT '판매 수',
    view_count INT NOT NULL DEFAULT 0 COMMENT '조회 수',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    INDEX idx_product_seller (seller_id),
    INDEX idx_product_category (category_id),
    INDEX idx_product_status (status),
    INDEX idx_product_delivery_type (delivery_type),
    INDEX idx_product_price (discount_price),
    INDEX idx_product_rating (average_rating),
    INDEX idx_product_sales (sales_count),
    INDEX idx_product_created (created_at),
    FULLTEXT INDEX idx_product_name_ft (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품';

-- Product Options 테이블
CREATE TABLE product_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    option_name VARCHAR(100) NOT NULL COMMENT '옵션명 (예: 색상: 블랙 / 사이즈: L)',
    option1 VARCHAR(50) NULL COMMENT '첫번째 옵션값 (예: 블랙)',
    option2 VARCHAR(50) NULL COMMENT '두번째 옵션값 (예: L)',
    add_price INT NOT NULL DEFAULT 0 COMMENT '추가 금액',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '재고 수량',
    is_available BOOLEAN NOT NULL DEFAULT TRUE COMMENT '판매 가능 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_option_product (product_id),
    INDEX idx_option_available (product_id, is_available)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품 옵션';

-- Product Images 테이블
CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    image_url VARCHAR(500) NOT NULL COMMENT '이미지 URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    is_thumbnail BOOLEAN NOT NULL DEFAULT FALSE COMMENT '썸네일 여부',
    image_type VARCHAR(20) NOT NULL DEFAULT 'MAIN' COMMENT '이미지 타입 (MAIN, DETAIL)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_image_product (product_id),
    INDEX idx_image_thumbnail (product_id, is_thumbnail)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='상품 이미지';

-- 초기 카테고리 데이터 (샘플)
INSERT INTO categories (name, parent_id, depth, sort_order) VALUES
-- 대분류 (depth: 0)
('패션의류', NULL, 0, 1),
('디지털/가전', NULL, 0, 2),
('식품', NULL, 0, 3),
('생활용품', NULL, 0, 4),
('뷰티', NULL, 0, 5);

-- 중분류 (depth: 1)
INSERT INTO categories (name, parent_id, depth, sort_order) VALUES
('여성의류', 1, 1, 1),
('남성의류', 1, 1, 2),
('신발', 1, 1, 3),
('컴퓨터/노트북', 2, 1, 1),
('휴대폰/태블릿', 2, 1, 2),
('TV/영상가전', 2, 1, 3),
('과일/채소', 3, 1, 1),
('정육/계란', 3, 1, 2),
('수산물', 3, 1, 3),
('세제/청소용품', 4, 1, 1),
('주방용품', 4, 1, 2),
('스킨케어', 5, 1, 1),
('메이크업', 5, 1, 2);

-- 소분류 (depth: 2) - 여성의류 하위
INSERT INTO categories (name, parent_id, depth, sort_order) VALUES
('원피스', 6, 2, 1),
('티셔츠', 6, 2, 2),
('블라우스', 6, 2, 3);
