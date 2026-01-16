package com.example.ecommerce.order.event;

import com.example.ecommerce.events.member.MemberWithdrawnEvent;
import com.example.ecommerce.order.domain.cart.repository.CartItemRepository;
import com.example.ecommerce.order.domain.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberEventConsumer {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @KafkaListener(topics = "member-events", groupId = "order-service-group")
    public void handleMemberEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        if (event instanceof MemberWithdrawnEvent memberWithdrawn) {
            handleMemberWithdrawn(memberWithdrawn);
        }
    }

    private void handleMemberWithdrawn(MemberWithdrawnEvent event) {
        log.info("회원 탈퇴 이벤트 수신: memberId={}", event.getMemberId());

        // Clean up member's cart data
        cartRepository.findByMemberId(event.getMemberId())
                .flatMap(cart -> cartItemRepository.deleteByCartId(cart.getId())
                        .then(cartRepository.delete(cart)))
                .subscribe(
                        null,
                        error -> log.error("회원 장바구니 정리 실패: memberId={}", event.getMemberId(), error),
                        () -> log.info("회원 장바구니 정리 완료: memberId={}", event.getMemberId())
                );
    }
}
