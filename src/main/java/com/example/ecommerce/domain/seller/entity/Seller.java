package com.example.ecommerce.domain.seller.entity;

import com.example.ecommerce.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("sellers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller extends BaseEntity {

    @Id
    private Long id;

    @Column("member_id")
    private Long memberId;

    @Column("business_name")
    private String businessName;

    @Column("business_number")
    private String businessNumber;

    @Column("representative_name")
    private String representativeName;

    @Column("business_address")
    private String businessAddress;

    @Column("contact_number")
    private String contactNumber;

    @Column("status")
    private SellerStatus status;

    @Builder
    public Seller(Long memberId, String businessName, String businessNumber,
                  String representativeName, String businessAddress, String contactNumber) {
        this.memberId = memberId;
        this.businessName = businessName;
        this.businessNumber = businessNumber;
        this.representativeName = representativeName;
        this.businessAddress = businessAddress;
        this.contactNumber = contactNumber;
        this.status = SellerStatus.PENDING;
    }

    // 비즈니스 로직

    /**
     * 판매자 승인 (관리자)
     */
    public Seller approve() {
        this.status = SellerStatus.ACTIVE;
        return this;
    }

    /**
     * 판매자 정지 (관리자)
     */
    public Seller suspend() {
        this.status = SellerStatus.SUSPENDED;
        return this;
    }

    /**
     * 판매자 탈퇴
     */
    public Seller withdraw() {
        this.status = SellerStatus.WITHDRAWN;
        return this;
    }

    /**
     * 판매자 정보 수정
     */
    public Seller updateInfo(String businessName, String businessAddress, String contactNumber) {
        if (businessName != null && !businessName.isBlank()) {
            this.businessName = businessName;
        }
        if (businessAddress != null && !businessAddress.isBlank()) {
            this.businessAddress = businessAddress;
        }
        if (contactNumber != null && !contactNumber.isBlank()) {
            this.contactNumber = contactNumber;
        }
        return this;
    }

    /**
     * 판매 가능 여부 확인
     */
    public boolean canSell() {
        return this.status.canSell();
    }

    /**
     * 활성 상태 여부 확인
     */
    public boolean isActive() {
        return this.status.isActive();
    }
}
