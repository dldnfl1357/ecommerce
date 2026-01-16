package com.example.ecommerce.member.domain.address.entity;

import com.example.ecommerce.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    @Id
    private Long id;

    @Column("member_id")
    private Long memberId;

    @Column("name")
    private String name;

    @Column("recipient")
    private String recipient;

    @Column("phone")
    private String phone;

    @Column("zip_code")
    private String zipCode;

    @Column("address")
    private String address;

    @Column("address_detail")
    private String addressDetail;

    @Column("is_default")
    private Boolean isDefault;

    @Column("delivery_request")
    private String deliveryRequest;

    @Builder
    public Address(Long id, Long memberId, String name, String recipient, String phone,
                   String zipCode, String address, String addressDetail, Boolean isDefault,
                   String deliveryRequest) {
        this.id = id;
        this.memberId = memberId;
        this.name = name;
        this.recipient = recipient;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.isDefault = isDefault != null ? isDefault : false;
        this.deliveryRequest = deliveryRequest;
    }

    public Address setAsDefault() {
        this.isDefault = true;
        return this;
    }

    public Address unsetDefault() {
        this.isDefault = false;
        return this;
    }

    public String getFullAddress() {
        return addressDetail != null
            ? address + " " + addressDetail
            : address;
    }
}
