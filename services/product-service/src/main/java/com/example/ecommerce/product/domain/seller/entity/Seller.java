package com.example.ecommerce.product.domain.seller.entity;

import com.example.ecommerce.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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

    @Column("contact_phone")
    private String contactPhone;

    @Column("contact_email")
    private String contactEmail;

    @Column("status")
    private SellerStatus status;

    @Column("approved_at")
    private LocalDateTime approvedAt;

    @Builder
    public Seller(Long memberId, String businessName, String businessNumber,
                  String representativeName, String contactPhone, String contactEmail) {
        this.memberId = memberId;
        this.businessName = businessName;
        this.businessNumber = businessNumber;
        this.representativeName = representativeName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.status = SellerStatus.PENDING;
    }

    public Seller approve() {
        this.status = SellerStatus.ACTIVE;
        this.approvedAt = LocalDateTime.now();
        return this;
    }

    public Seller suspend() {
        this.status = SellerStatus.SUSPENDED;
        return this;
    }

    public Seller reject() {
        this.status = SellerStatus.REJECTED;
        return this;
    }

    public boolean isActive() {
        return this.status == SellerStatus.ACTIVE;
    }

    public boolean canSellProducts() {
        return this.status == SellerStatus.ACTIVE;
    }
}
