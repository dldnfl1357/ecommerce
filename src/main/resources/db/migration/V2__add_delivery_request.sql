-- addresses 테이블에 배송 요청사항 컬럼 추가
ALTER TABLE addresses ADD COLUMN delivery_request VARCHAR(255) AFTER is_default;
