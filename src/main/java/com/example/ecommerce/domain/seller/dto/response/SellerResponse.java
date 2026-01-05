package com.example.ecommerce.domain.seller.dto.response;

import com.example.ecommerce.domain.seller.entity.Seller;
import com.example.ecommerce.domain.seller.entity.SellerStatus;
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
public class SellerResponse {

    private Long id;
    private Long memberId;
    private String businessName;
    private String maskedBusinessNumber;
    private String representativeName;
    private String businessAddress;
    private String contactNumber;
    private SellerStatus status;
    private String statusDisplayName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Entity -> Response DTO 변환
     */
    public static SellerResponse from(Seller seller) {
        return SellerResponse.builder()
                .id(seller.getId())
                .memberId(seller.getMemberId())
                .businessName(seller.getBusinessName())
                .maskedBusinessNumber(maskBusinessNumber(seller.getBusinessNumber()))
                .representativeName(seller.getRepresentativeName())
                .businessAddress(seller.getBusinessAddress())
                .contactNumber(formatContactNumber(seller.getContactNumber()))
                .status(seller.getStatus())
                .statusDisplayName(seller.getStatus().getDisplayName())
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .build();
    }

    /**
     * 간략 정보 (목록 조회용)
     */
    public static SellerResponse summary(Seller seller) {
        return SellerResponse.builder()
                .id(seller.getId())
                .businessName(seller.getBusinessName())
                .status(seller.getStatus())
                .statusDisplayName(seller.getStatus().getDisplayName())
                .build();
    }

    /**
     * 사업자등록번호 마스킹 (123-45-67890 -> 123-**-****0)
     */
    private static String maskBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.length() < 10) {
            return businessNumber;
        }
        // 하이픈 없는 10자리를 포맷팅하면서 마스킹
        return businessNumber.substring(0, 3) + "-**-****" + businessNumber.substring(9);
    }

    /**
     * 연락처 포맷팅 (0212345678 -> 02-1234-5678)
     */
    private static String formatContactNumber(String contactNumber) {
        if (contactNumber == null) {
            return null;
        }
        // 이미 포맷팅되어 있으면 그대로 반환
        if (contactNumber.contains("-")) {
            return contactNumber;
        }
        // 숫자만 있는 경우 포맷팅
        if (contactNumber.length() == 10) {
            return contactNumber.substring(0, 2) + "-" + contactNumber.substring(2, 6) + "-" + contactNumber.substring(6);
        } else if (contactNumber.length() == 11) {
            return contactNumber.substring(0, 3) + "-" + contactNumber.substring(3, 7) + "-" + contactNumber.substring(7);
        }
        return contactNumber;
    }
}
