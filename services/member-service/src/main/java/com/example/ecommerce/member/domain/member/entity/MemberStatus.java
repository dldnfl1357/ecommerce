package com.example.ecommerce.member.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberStatus {
    ACTIVE("활성", "정상적으로 이용 가능한 회원"),
    DORMANT("휴면", "1년 이상 미접속 회원"),
    WITHDRAWN("탈퇴", "탈퇴한 회원"),
    SUSPENDED("정지", "이용 정지된 회원");

    private final String displayName;
    private final String description;

    public boolean canLogin() {
        return this == ACTIVE;
    }

    public boolean isDormant() {
        return this == DORMANT;
    }

    public boolean isWithdrawn() {
        return this == WITHDRAWN;
    }
}
