package com.example.ecommerce.domain.member.dto.response;

import com.example.ecommerce.domain.member.entity.Address;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private Long memberId;
    private String name;
    private String recipient;
    private String phone;
    private String zipCode;
    private String address;
    private String addressDetail;
    private String fullAddress;
    private Boolean isDefault;
    private String deliveryRequest;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // 정적 팩토리 메서드
    public static AddressResponse from(Address address) {
        return AddressResponse.builder()
            .id(address.getId())
            .memberId(address.getMemberId())
            .name(address.getName())
            .recipient(address.getRecipient())
            .phone(address.getPhone())
            .zipCode(address.getZipCode())
            .address(address.getAddress())
            .addressDetail(address.getAddressDetail())
            .fullAddress(address.getFullAddress())
            .isDefault(address.getIsDefault())
            .deliveryRequest(address.getDeliveryRequest())
            .createdAt(address.getCreatedAt())
            .updatedAt(address.getUpdatedAt())
            .build();
    }
}
