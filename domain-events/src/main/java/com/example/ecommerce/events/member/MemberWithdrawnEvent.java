package com.example.ecommerce.events.member;

import com.example.ecommerce.events.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 회원 탈퇴 이벤트
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWithdrawnEvent extends DomainEvent {

    private Long memberId;
    private String email;

    public static MemberWithdrawnEvent of(Long memberId, String email) {
        MemberWithdrawnEvent event = MemberWithdrawnEvent.builder()
                .memberId(memberId)
                .email(email)
                .build();
        event.init(String.valueOf(memberId), "Member");
        return event;
    }

    @Override
    public String getEventType() {
        return "MEMBER_WITHDRAWN";
    }
}
