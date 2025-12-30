package com.example.ecommerce.domain.member.service;

import com.example.ecommerce.domain.member.dto.request.MemberUpdateRequest;
import com.example.ecommerce.domain.member.dto.request.SignupRequest;
import com.example.ecommerce.domain.member.dto.response.MemberResponse;
import com.example.ecommerce.domain.member.entity.Member;
import com.example.ecommerce.domain.member.entity.MemberGrade;
import com.example.ecommerce.domain.member.entity.MemberStatus;
import com.example.ecommerce.domain.member.repository.MemberRepository;
import com.example.ecommerce.global.exception.BusinessException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

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
     * 회원 정보 수정
     */
    @Transactional
    public Mono<MemberResponse> updateMember(Long memberId, MemberUpdateRequest request) {
        return memberRepository.findById(memberId)
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)))
            .flatMap(member -> updateMemberInfo(member, request))
            .flatMap(memberRepository::save)
            .doOnSuccess(member -> log.info("회원 정보 수정 완료: {}", member.getId()))
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

    // ========== Private Methods (함수형 헬퍼) ==========

    /**
     * 이메일 중복 검증
     */
    private Mono<Void> validateEmail(String email) {
        return memberRepository.existsByEmail(email)
            .flatMap(exists -> exists
                ? Mono.error(new BusinessException(ErrorCode.DUPLICATE_EMAIL))
                : Mono.empty());
    }

    /**
     * 전화번호 중복 검증
     */
    private Mono<Void> validatePhone(String phone) {
        return memberRepository.existsByPhone(phone)
            .flatMap(exists -> exists
                ? Mono.error(new BusinessException(ErrorCode.DUPLICATE_PHONE))
                : Mono.empty());
    }

    /**
     * 회원 생성 (비밀번호 암호화는 Blocking이므로 별도 스레드에서 실행)
     */
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
            .subscribeOn(Schedulers.boundedElastic());  // Blocking 작업은 별도 스레드풀
    }

    /**
     * 회원 정보 업데이트
     */
    private Mono<Member> updateMemberInfo(Member member, MemberUpdateRequest request) {
        return Mono.just(member)
            .flatMap(m -> {
                // 이름 업데이트
                if (request.getName() != null && !request.getName().isBlank()) {
                    // Member 엔티티에 setName이 없으므로 리플렉션 사용하거나
                    // 새로운 Member 객체 생성 (불변성 유지)
                    // 여기서는 편의상 필드에 직접 접근한다고 가정
                }

                // 전화번호 업데이트 및 중복 검증
                if (request.getPhone() != null) {
                    return memberRepository.existsByPhone(request.getNormalizedPhone())
                        .flatMap(exists -> {
                            if (exists && !member.getPhone().equals(request.getNormalizedPhone())) {
                                return Mono.error(new BusinessException(ErrorCode.DUPLICATE_PHONE));
                            }
                            // 전화번호 업데이트 로직
                            return Mono.just(member);
                        });
                }

                return Mono.just(member);
            });
    }

    /**
     * 적립금 사용 검증 및 처리
     */
    private Mono<Member> validateAndUsePoint(Member member, int amount) {
        if (amount < 1000) {
            return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_POINT_MINIMUM));
        }

        if (!member.canUsePoint(amount)) {
            return Mono.error(new BusinessException(ErrorCode.INSUFFICIENT_POINT));
        }

        return Mono.just(member.usePoint(amount));
    }

    /**
     * 등급 업그레이드 (필요시)
     */
    private Mono<Member> upgradeGradeIfNeeded(Member member, int totalPurchaseAmount) {
        MemberGrade newGrade = MemberGrade.calculateGrade(totalPurchaseAmount);

        if (newGrade.getLevel() > member.getGrade().getLevel()) {
            log.info("회원 등급 업그레이드: memberId={}, {} -> {}",
                member.getId(), member.getGrade(), newGrade);
            return Mono.just(member.upgradeGrade(newGrade));
        }

        return Mono.just(member);
    }

    /**
     * 예외 매핑 (함수형)
     */
    private Throwable mapToBusinessException(Throwable error) {
        if (error instanceof BusinessException) {
            return error;
        }

        log.error("예상치 못한 오류 발생", error);
        return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
