package com.example.ecommerce.member.domain.member.service;

import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.common.kafka.Topics;
import com.example.ecommerce.common.kafka.publisher.EventPublisher;
import com.example.ecommerce.events.member.MemberCreatedEvent;
import com.example.ecommerce.events.member.MemberWithdrawnEvent;
import com.example.ecommerce.member.domain.member.dto.request.SignupRequest;
import com.example.ecommerce.member.domain.member.dto.response.MemberResponse;
import com.example.ecommerce.member.domain.member.entity.Member;
import com.example.ecommerce.member.domain.member.entity.MemberGrade;
import com.example.ecommerce.member.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    /**
     * 회원 가입 (함수형 스타일)
     */
    @Transactional
    public Mono<MemberResponse> signup(SignupRequest request) {
        return validateEmail(request.getEmail())
            .then(validatePhone(request.getNormalizedPhone()))
            .then(createMember(request))
            .flatMap(memberRepository::save)
            .doOnSuccess(member -> log.info("회원 가입 완료: {}", member.getEmail()))
            .flatMap(this::publishMemberCreatedEvent)
            .map(MemberResponse::from)
            .onErrorMap(this::mapToBusinessException);
    }

    /**
     * 회원 조회
     */
    public Mono<MemberResponse> getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .map(MemberResponse::from);
    }

    /**
     * 이메일로 회원 조회
     */
    public Mono<MemberResponse> getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .map(MemberResponse::from);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public Mono<Void> withdraw(Long memberId) {
        return memberRepository.findById(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .map(Member::withdraw)
            .flatMap(memberRepository::save)
            .doOnSuccess(member -> log.info("회원 탈퇴 완료: {}", member.getId()))
            .flatMap(this::publishMemberWithdrawnEvent)
            .then();
    }

    /**
     * 적립금 사용
     */
    @Transactional
    public Mono<Void> usePoint(Long memberId, int amount) {
        return memberRepository.findById(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .flatMap(member -> validateAndUsePoint(member, amount))
            .flatMap(memberRepository::save)
            .then();
    }

    /**
     * 적립금 적립
     */
    @Transactional
    public Mono<Void> earnPoint(Long memberId, int amount) {
        return memberRepository.findById(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .map(member -> member.earnPoint(amount))
            .flatMap(memberRepository::save)
            .doOnSuccess(member -> log.info("적립금 적립 완료: memberId={}, amount={}", memberId, amount))
            .then();
    }

    /**
     * 회원 등급 업데이트 (구매 금액 기준)
     */
    @Transactional
    public Mono<Void> updateGrade(Long memberId, int totalPurchaseAmount) {
        return memberRepository.findById(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .flatMap(member -> upgradeGradeIfNeeded(member, totalPurchaseAmount))
            .flatMap(memberRepository::save)
            .then();
    }

    /**
     * 로켓와우 구독
     */
    @Transactional
    public Mono<MemberResponse> subscribeRocketWow(Long memberId) {
        return memberRepository.findById(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .map(Member::subscribeRocketWow)
            .flatMap(memberRepository::save)
            .doOnSuccess(member -> log.info("로켓와우 구독 완료: {}", member.getId()))
            .map(MemberResponse::from);
    }

    /**
     * 로켓와우 취소
     */
    @Transactional
    public Mono<MemberResponse> cancelRocketWow(Long memberId) {
        return memberRepository.findById(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .map(Member::cancelRocketWow)
            .flatMap(memberRepository::save)
            .doOnSuccess(member -> log.info("로켓와우 취소 완료: {}", member.getId()))
            .map(MemberResponse::from);
    }

    // ========== Private Methods ==========

    private Mono<Void> validateEmail(String email) {
        return memberRepository.existsByEmail(email)
            .flatMap(exists -> exists
                ? Mono.error(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
                : Mono.empty());
    }

    private Mono<Void> validatePhone(String phone) {
        return memberRepository.existsByPhone(phone)
            .flatMap(exists -> exists
                ? Mono.error(new BusinessException(ErrorCode.DUPLICATE_PHONE))
                : Mono.empty());
    }

    private Mono<Member> createMember(SignupRequest request) {
        return Mono.fromCallable(() -> {
                String encodedPassword = passwordEncoder.encode(request.getPassword());
                return Member.builder()
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .name(request.getName())
                    .phone(request.getNormalizedPhone())
                    .build();
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Member> validateAndUsePoint(Member member, int amount) {
        if (amount < 1000) {
            return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_POINT_MINIMUM));
        }

        if (!member.canUsePoint(amount)) {
            return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_POINT));
        }

        return Mono.just(member.usePoint(amount));
    }

    private Mono<Member> upgradeGradeIfNeeded(Member member, int totalPurchaseAmount) {
        MemberGrade newGrade = MemberGrade.calculateGrade(totalPurchaseAmount);

        if (newGrade.getLevel() > member.getGrade().getLevel()) {
            log.info("회원 등급 업그레이드: memberId={}, {} -> {}",
                member.getId(), member.getGrade(), newGrade);
            return Mono.just(member.upgradeGrade(newGrade));
        }

        return Mono.just(member);
    }

    private Mono<Member> publishMemberCreatedEvent(Member member) {
        MemberCreatedEvent event = MemberCreatedEvent.of(
            member.getId(),
            member.getEmail(),
            member.getName(),
            member.getPhone()
        );
        return eventPublisher.publish(Topics.MEMBER_EVENTS, event)
            .thenReturn(member);
    }

    private Mono<Member> publishMemberWithdrawnEvent(Member member) {
        MemberWithdrawnEvent event = MemberWithdrawnEvent.of(
            member.getId(),
            member.getEmail()
        );
        return eventPublisher.publish(Topics.MEMBER_EVENTS, event)
            .thenReturn(member);
    }

    private Throwable mapToBusinessException(Throwable error) {
        if (error instanceof BusinessException) {
            return error;
        }

        log.error("예상치 못한 오류 발생", error);
        return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
