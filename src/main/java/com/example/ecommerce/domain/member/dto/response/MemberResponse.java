package com.example.ecommerce.domain.member.dto.response;

import com.example.ecommerce.domain.member.entity.Member;
import com.example.ecommerce.domain.member.entity.MemberGrade;
import com.example.ecommerce.domain.member.entity.MemberStatus;
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
public class MemberResponse {

    private Long id;
    private String email;
    private String maskedEmail;  // 마스킹된 이메일
    private String name;
    private String phone;
    private MemberGrade grade;
    private String gradeDisplayName;
    private Integer point;
    private MemberStatus status;
    private String statusDisplayName;
    private Boolean rocketWowActive;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime rocketWowExpiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드 (함수형 스타일)
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
            .id(member.getId())
            .email(member.getEmail())
            .maskedEmail(member.getMaskedEmail())
            .name(member.getName())
            .phone(member.getPhone())
            .grade(member.getGrade())
            .gradeDisplayName(member.getGrade().getDisplayName())
            .point(member.getPoint())
            .status(member.getStatus())
            .statusDisplayName(member.getStatus().getDisplayName())
            .rocketWowActive(member.isRocketWowActive())
            .rocketWowExpiresAt(member.getRocketWowExpiresAt())
            .lastLoginAt(member.getLastLoginAt())
            .createdAt(member.getCreatedAt())
            .build();
    }

    // 간단한 정보만 포함 (목록 조회용)
    public static MemberResponse summary(Member member) {
        return MemberResponse.builder()
            .id(member.getId())
            .maskedEmail(member.getMaskedEmail())
            .name(member.getName())
            .grade(member.getGrade())
            .gradeDisplayName(member.getGrade().getDisplayName())
            .rocketWowActive(member.isRocketWowActive())
            .build();
    }
}
