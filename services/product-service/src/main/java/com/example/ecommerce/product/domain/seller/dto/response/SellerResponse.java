package com.example.ecommerce.product.domain.seller.dto.response;

import com.example.ecommerce.product.domain.seller.entity.Seller;
import com.example.ecommerce.product.domain.seller.entity.SellerStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SellerResponse {

    private Long id;
    private Long memberId;
    private String businessName;
    private String businessNumber;
    private String representativeName;
    private String contactPhone;
    private String contactEmail;
    private SellerStatus status;
    private String statusDisplayName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static SellerResponse from(Seller seller) {
        return SellerResponse.builder()
                .id(seller.getId())
                .memberId(seller.getMemberId())
                .businessName(seller.getBusinessName())
                .businessNumber(seller.getBusinessNumber())
                .representativeName(seller.getRepresentativeName())
                .contactPhone(seller.getContactPhone())
                .contactEmail(seller.getContactEmail())
                .status(seller.getStatus())
                .statusDisplayName(seller.getStatus().getDisplayName())
                .approvedAt(seller.getApprovedAt())
                .createdAt(seller.getCreatedAt())
                .build();
    }
}
