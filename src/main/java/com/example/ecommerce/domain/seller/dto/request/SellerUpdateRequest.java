package com.example.ecommerce.domain.seller.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SellerUpdateRequest {

    @Size(min = 2, max = 100, message = "상호명은 2자 이상 100자 이하여야 합니다")
    private String businessName;

    @Size(max = 255, message = "사업장 주소는 255자 이하여야 합니다")
    private String businessAddress;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "연락처 형식이 올바르지 않습니다 (예: 02-1234-5678)")
    private String contactNumber;

    /**
     * 연락처 정규화 (하이픈 제거)
     */
    public String getNormalizedContactNumber() {
        return contactNumber != null ? contactNumber.replaceAll("-", "") : null;
    }
}
