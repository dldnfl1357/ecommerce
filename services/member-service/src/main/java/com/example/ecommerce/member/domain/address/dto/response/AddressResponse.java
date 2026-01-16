package com.example.ecommerce.member.domain.address.dto.response;

import com.example.ecommerce.member.domain.address.entity.Address;
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

    public static AddressResponse from(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
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
                .build();
    }
}
