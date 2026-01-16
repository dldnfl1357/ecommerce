package com.example.ecommerce.product.domain.seller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SellerRegisterRequest {

    @NotBlank(message = "상호명은 필수입니다")
    @Size(max = 100, message = "상호명은 100자 이하여야 합니다")
    private String businessName;

    @NotBlank(message = "사업자등록번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다 (000-00-00000)")
    private String businessNumber;

    @NotBlank(message = "대표자명은 필수입니다")
    @Size(max = 50, message = "대표자명은 50자 이하여야 합니다")
    private String representativeName;

    @NotBlank(message = "연락처는 필수입니다")
    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String contactPhone;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String contactEmail;

    public String getNormalizedBusinessNumber() {
        return businessNumber != null ? businessNumber.replaceAll("-", "") : null;
    }
}
