package com.example.ecommerce.member.domain.address.dto.request;

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
public class AddressCreateRequest {

    @NotBlank(message = "배송지명은 필수입니다")
    @Size(max = 100, message = "배송지명은 100자 이하여야 합니다")
    private String name;

    @NotBlank(message = "수령인은 필수입니다")
    @Size(max = 100, message = "수령인은 100자 이하여야 합니다")
    private String recipient;

    @NotBlank(message = "연락처는 필수입니다")
    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;

    @NotBlank(message = "우편번호는 필수입니다")
    @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다")
    private String zipCode;

    @NotBlank(message = "주소는 필수입니다")
    @Size(max = 255, message = "주소는 255자 이하여야 합니다")
    private String address;

    @Size(max = 255, message = "상세주소는 255자 이하여야 합니다")
    private String addressDetail;

    private Boolean isDefault;

    @Size(max = 255, message = "배송 요청사항은 255자 이하여야 합니다")
    private String deliveryRequest;

    public String getNormalizedPhone() {
        return phone != null ? phone.replaceAll("-", "") : null;
    }
}
