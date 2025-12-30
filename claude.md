# E-Commerce Project (Coupang Clone) - Claude Context

## 프로젝트 개요
- **목적**: 쿠팡 클론 E-commerce 플랫폼 (학습/포트폴리오용)
- **주요 기능**: 회원, 상품, 주문, 결제, 배송, 쿠폰, 리뷰, 판매자 정산
- **목표**: WebFlux 기반 고성능 비동기 리액티브 시스템 구축

## 핵심 기술 스택

### Backend Framework
- **Spring Boot**: 3.2.0
- **Java**: 17
- **Reactive Stack**: Spring WebFlux (NOT Spring MVC)
- **Database Access**: R2DBC MySQL (NOT JPA/Hibernate)
  - Repository: `ReactiveCrudRepository` 상속
  - 반환 타입: `Mono<T>`, `Flux<T>` (NOT `Optional`, `List`)

### Database & Cache
- **Database**: MySQL 8.0 (R2DBC Driver)
- **Cache**: Redis (Reactive)
- **Migration**: Flyway (JDBC - 마이그레이션 전용)
- **Test DB**: H2 (R2DBC)

### Security & Auth
- **Authentication**: JWT (Access Token + Refresh Token)
- **Security**: Spring Security (WebFlux)
- **Password**: BCrypt (Schedulers.boundedElastic()에서 실행)
- **Session**: Redis 기반 Refresh Token 저장

### Testing & Documentation
- **Testing**: JUnit 5, Mockito, Reactor Test (StepVerifier)
- **API Docs**: Spring REST Docs + Asciidoctor
- **Controller Test**: @WebFluxTest + WebTestClient
- **Test Strategy**: API당 성공 1개 + 실패 5개 작성

## 아키텍처 원칙

### 1. 완전한 Non-Blocking Stack
```
Controller (Mono/Flux)
  → Service (Reactive Chains)
    → Repository (R2DBC)
      → Database
```

### 2. Blocking 작업 처리
Blocking 작업(BCrypt, JWT)은 반드시 별도 스레드풀에서 실행:
```java
return Mono.fromCallable(() -> passwordEncoder.encode(password))
    .subscribeOn(Schedulers.boundedElastic());
```

### 3. 함수형 프로그래밍 스타일
- 명령형 코드 대신 함수형 체이닝 사용
- `flatMap`, `map`, `switchIfEmpty`, `doOnSuccess`, `onErrorMap` 활용
- Stream API, Optional, Lambda 적극 사용

## 코딩 컨벤션

### Layered Architecture
```
controller/     - @RestController, WebFlux 엔드포인트
  dto/
    request/    - 요청 DTO (@Valid 검증)
    response/   - 응답 DTO (static factory method)
service/        - @Service, 비즈니스 로직 (Reactive)
repository/     - ReactiveCrudRepository 인터페이스
entity/         - @Table, R2DBC 엔티티
```

### 네이밍 규칙
- **Entity**: 도메인 객체명 (Member, Product, Order)
- **Request DTO**: `{Action}Request` (SignupRequest, OrderCreateRequest)
- **Response DTO**: `{Domain}Response` (MemberResponse, ProductResponse)
- **Repository**: `{Entity}Repository`
- **Service**: `{Domain}Service`
- **Controller**: `{Domain}Controller`

### DTO 규칙
1. **Request/Response 완전 분리** (절대 Entity를 직접 노출하지 않음)
2. Request DTO: Jakarta Validation 어노테이션 필수
   ```java
   @NotBlank(message = "이메일은 필수입니다")
   @Email(message = "올바른 이메일 형식이 아닙니다")
   private String email;
   ```
3. Response DTO: Static factory method 제공
   ```java
   public static MemberResponse from(Member member) {
       return MemberResponse.builder()...build();
   }
   ```

### Entity 규칙
1. **BaseEntity 상속** (createdAt, updatedAt 자동 관리)
2. **Builder 패턴** 사용
3. **비즈니스 로직 메서드** 포함 (도메인 주도 설계)
   ```java
   public Member usePoint(int amount) {
       if (this.point < amount) {
           throw new IllegalStateException("적립금이 부족합니다.");
       }
       this.point -= amount;
       return this;  // 메서드 체이닝
   }
   ```
4. R2DBC 어노테이션 사용:
   - `@Table("table_name")`
   - `@Id`
   - `@Column("column_name")`

### Service 규칙
1. **함수형 체이닝**: 명령형 if/for 대신 Reactive operators 사용
2. **에러 처리**: `onErrorMap(this::mapToBusinessException)`
3. **로깅**: `doOnSuccess`, `doOnError`로 사이드 이펙트 처리
4. **트랜잭션**: `@Transactional` (R2DBC는 자동 지원)

### Controller 규칙
1. **응답 포맷**: 모든 API는 `ApiResponse<T>` 래퍼 사용
   ```java
   return service.method()
       .map(response -> ApiResponse.success(response, "메시지"));
   ```
2. **인증**: `@RequestAttribute("memberId") Long memberId` (JWT에서 추출)
3. **검증**: `@Valid @RequestBody` 자동 검증
4. **로깅**: 요청/성공/실패 모두 로깅

## 에러 처리

### ErrorCode Enum
모든 비즈니스 에러는 `ErrorCode` enum으로 관리:
```java
MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다.")
```

### BusinessException
```java
throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
```

### GlobalExceptionHandler
- WebFlux 전역 예외 처리
- BusinessException → ErrorCode 기반 응답
- 기타 예외 → 500 Internal Server Error

## 테스트 전략

### Controller Test (WebFluxTest)
```java
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@WebFluxTest(AuthController.class)
@AutoConfigureRestDocs
class AuthControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MemberService memberService;

    @Test
    void signup_success() {
        // REST Docs 문서화 포함
        webTestClient.post()
            .uri("/api/v1/auth/signup")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .consumeWith(document("auth/signup-success", ...));
    }
}
```

### Service Test (Mockito + StepVerifier)
```java
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    void signup_success() {
        given(memberRepository.save(any()))
            .willReturn(Mono.just(member));

        StepVerifier.create(memberService.signup(request))
            .assertNext(response -> {
                assertThat(response.getEmail()).isEqualTo("test@example.com");
            })
            .verifyComplete();
    }
}
```

### 테스트 커버리지 목표
- 각 API: 성공 1개 + 실패 5개 (총 6개)
- 성공 케이스: REST Docs 문서화 필수
- 실패 케이스: 다양한 에러 시나리오 커버

## 데이터베이스

### 스키마 관리
- **Flyway**: `src/main/resources/db/migration/V{version}__{description}.sql`
- 버전 관리: V1__init_schema.sql, V2__add_member_table.sql

### 인덱스 전략
```sql
-- 조회 성능 최적화
CREATE INDEX idx_member_email ON members(email);
CREATE INDEX idx_member_phone ON members(phone);
CREATE INDEX idx_member_status ON members(status);
```

## 비즈니스 규칙

### Member Domain
- 회원 등급: BRONZE (기본) → SILVER (10만원) → GOLD (50만원) → VIP (100만원)
- 적립금: 최소 사용 금액 1,000원
- 로켓와우: 월 구독 서비스, 무료배송 혜택
- 휴면 전환: 1년 미접속 시 자동 휴면
- 최대 배송지: 10개

### Address Domain
- 첫 번째 배송지: 자동으로 기본 배송지 설정
- 기본 배송지: 다른 배송지가 있으면 삭제 불가
- 배송지 개수: 최대 10개 제한

## 개발 명령어

### 빌드 & 실행
```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 특정 패키지 테스트
./gradlew test --tests "com.example.ecommerce.domain.member.*"

# REST Docs 생성
./gradlew asciidoctor

# 전체 빌드 (테스트 + 문서)
./gradlew clean build
```

### Docker
```bash
# MySQL + Redis 실행
docker-compose up -d

# 중지
docker-compose down
```

### Kubernetes (로컬)
```bash
kubectl apply -f k8s/
```

## 주의사항 ⚠️

### 안티패턴
1. ❌ JPA/Hibernate 사용 (R2DBC만 사용)
2. ❌ Blocking 코드를 메인 스레드에서 실행
3. ❌ Entity를 Controller에서 직접 반환
4. ❌ Optional/List 반환 (Mono/Flux만 사용)
5. ❌ `block()` 호출 (테스트 코드 제외)

### 반드시 지킬 것
1. ✅ 모든 DB 작업은 R2DBC Reactive
2. ✅ Blocking 작업은 `Schedulers.boundedElastic()`
3. ✅ Request/Response DTO 철저히 분리
4. ✅ 함수형 프로그래밍 스타일
5. ✅ 테스트 코드 작성 (성공 1 + 실패 5)

## 디렉토리 구조
```
src/
├── main/
│   ├── java/com/example/ecommerce/
│   │   ├── domain/
│   │   │   ├── member/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/
│   │   │   │   │   └── response/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   ├── product/
│   │   │   ├── order/
│   │   │   └── ...
│   │   └── global/
│   │       ├── auth/           # JWT, Security
│   │       ├── common/         # ApiResponse, BaseEntity
│   │       ├── config/         # 설정 클래스
│   │       └── exception/      # ErrorCode, GlobalExceptionHandler
│   └── resources/
│       ├── application.yml
│       └── db/migration/       # Flyway scripts
└── test/
    └── java/com/example/ecommerce/
        └── domain/member/
            ├── controller/     # WebFluxTest
            └── service/        # Mockito Test
```

## 참고 문서
- **프로젝트 계획**: `/plan/` 디렉토리 (6개 파일)
- **완료 내역**: `done.md`
- **README**: `readme.md`

## 다음 구현 예정 도메인
1. ✅ Member (완료)
2. ⏳ Product (상품 관리)
3. ⏳ Inventory (재고 관리)
4. ⏳ Cart (장바구니)
5. ⏳ Order (주문)
6. ⏳ Payment (결제)
7. ⏳ Delivery (배송)
8. ⏳ Coupon (쿠폰)
9. ⏳ Review (리뷰)
10. ⏳ Seller (판매자)
