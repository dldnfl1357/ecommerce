-- Seller 테이블 생성
-- 판매자 정보를 관리하는 테이블

CREATE TABLE sellers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE COMMENT '회원 ID (1:1 관계)',
    business_name VARCHAR(100) NOT NULL COMMENT '상호명',
    business_number VARCHAR(20) NOT NULL UNIQUE COMMENT '사업자등록번호',
    representative_name VARCHAR(50) NOT NULL COMMENT '대표자명',
    business_address VARCHAR(255) NOT NULL COMMENT '사업장 주소',
    contact_number VARCHAR(20) NOT NULL COMMENT '연락처',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '판매자 상태 (PENDING, ACTIVE, SUSPENDED, WITHDRAWN)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    INDEX idx_seller_member (member_id),
    INDEX idx_seller_status (status),
    INDEX idx_seller_business_number (business_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='판매자 정보';
