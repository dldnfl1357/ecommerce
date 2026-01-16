package com.example.ecommerce.events.member;

import com.example.ecommerce.events.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 회원 생성 이벤트
 */
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCreatedEvent extends DomainEvent {

    private Long memberId;
    private String email;
    private String name;
    private String phone;

    public static MemberCreatedEvent of(Long memberId, String email, String name, String phone) {
        MemberCreatedEvent event = MemberCreatedEvent.builder()
                .memberId(memberId)
                .email(email)
                .name(name)
                .phone(phone)
                .build();
        event.init(String.valueOf(memberId), "Member");
        return event;
    }

    @Override
    public String getEventType() {
        return "MEMBER_CREATED";
    }
}
