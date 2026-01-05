package com.example.ecommerce.domain.seller.dto.request;

import com.example.ecommerce.domain.seller.entity.Seller;
import jakarta.validation.constraints.NotBlank;
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
public class SellerRegisterRequest {

    @NotBlank(message = "상호명은 필수입니다")
    @Size(min = 2, max = 100, message = "상호명은 2자 이상 100자 이하여야 합니다")
    private String businessName;

    @NotBlank(message = "사업자등록번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다 (예: 123-45-67890)")
    private String businessNumber;

    @NotBlank(message = "대표자명은 필수입니다")
    @Size(min = 2, max = 50, message = "대표자명은 2자 이상 50자 이하여야 합니다")
    private String representativeName;

    @NotBlank(message = "사업장 주소는 필수입니다")
    @Size(max = 255, message = "사업장 주소는 255자 이하여야 합니다")
    private String businessAddress;

    @NotBlank(message = "연락처는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "연락처 형식이 올바르지 않습니다 (예: 02-1234-5678)")
    private String contactNumber;

    /**
     * Entity로 변환
     */
    public Seller toEntity(Long memberId) {
        return Seller.builder()
                .memberId(memberId)
                .businessName(this.businessName)
                .businessNumber(getNormalizedBusinessNumber())
                .representativeName(this.representativeName)
                .businessAddress(this.businessAddress)
                .contactNumber(getNormalizedContactNumber())
                .build();
    }

    /**
     * 사업자등록번호 정규화 (하이픈 제거)
     */
    public String getNormalizedBusinessNumber() {
        return businessNumber != null ? businessNumber.replaceAll("-", "") : null;
    }

    /**
     * 연락처 정규화 (하이픈 제거)
     */
    public String getNormalizedContactNumber() {
        return contactNumber != null ? contactNumber.replaceAll("-", "") : null;
    }
}
