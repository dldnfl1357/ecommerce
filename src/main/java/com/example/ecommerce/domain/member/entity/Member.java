package com.example.ecommerce.domain.member.entity;

import com.example.ecommerce.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    private Long id;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("name")
    private String name;

    @Column("phone")
    private String phone;

    @Column("grade")
    private MemberGrade grade;

    @Column("point")
    private Integer point;

    @Column("status")
    private MemberStatus status;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    @Column("withdrawn_at")
    private LocalDateTime withdrawnAt;

    // 로켓와우 회원 정보
    @Column("rocket_wow_active")
    private Boolean rocketWowActive;

    @Column("rocket_wow_started_at")
    private LocalDateTime rocketWowStartedAt;

    @Column("rocket_wow_expires_at")
    private LocalDateTime rocketWowExpiresAt;

    @Column("rocket_wow_auto_renewal")
    private Boolean rocketWowAutoRenewal;

    @Builder
    public Member(String email, String password, String name, String phone) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.grade = MemberGrade.BRONZE;
        this.point = 0;
        this.status = MemberStatus.ACTIVE;
        this.rocketWowActive = false;
        this.rocketWowAutoRenewal = false;
    }

    // 비즈니스 로직 - 함수형 스타일
    public Member login() {
        this.lastLoginAt = LocalDateTime.now();
        return this;
    }

    public Member usePoint(int amount) {
        if (this.point < amount) {
            throw new IllegalStateException("적립금이 부족합니다. 보유: " + this.point + ", 사용 요청: " + amount);
        }
        this.point -= amount;
        return this;
    }

    public Member earnPoint(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("적립금은 0 이상이어야 합니다.");
        }
        this.point += amount;
        return this;
    }

    public Member upgradeGrade(MemberGrade newGrade) {
        if (newGrade.getLevel() <= this.grade.getLevel()) {
            throw new IllegalStateException(
                String.format("등급 업그레이드만 가능합니다. 현재: %s, 요청: %s", this.grade, newGrade)
            );
        }
        this.grade = newGrade;
        return this;
    }

    public Member withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
        return this;
    }

    public Member subscribeRocketWow() {
        this.rocketWowActive = true;
        this.rocketWowStartedAt = LocalDateTime.now();
        this.rocketWowExpiresAt = LocalDateTime.now().plusMonths(1);
        return this;
    }

    public Member cancelRocketWow() {
        this.rocketWowActive = false;
        this.rocketWowAutoRenewal = false;
        return this;
    }

    // 조회 메서드
    public boolean isRocketWowActive() {
        return rocketWowActive != null
            && rocketWowActive
            && rocketWowExpiresAt != null
            && rocketWowExpiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    public boolean canUsePoint(int amount) {
        return this.point >= amount;
    }

    // 이메일 마스킹 (개인정보 보호)
    @Transient
    public String getMaskedEmail() {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        int visibleChars = Math.min(3, localPart.length());
        String masked = localPart.substring(0, visibleChars) + "***";

        return masked + "@" + domain;
    }
}
