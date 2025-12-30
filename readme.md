# 🛒 이커머스 클론 프로젝트 (Coupang Clone)

> **Java 17 + Spring WebFlux + R2DBC + MySQL** 기반의 고성능 비동기 리액티브 이커머스 플랫폼

---

## 📋 프로젝트 개요

### 목표
쿠팡과 같은 대규모 이커머스의 핵심 기능을 **WebFlux 기반 리액티브 아키텍처**로 구현하여 고성능 비동기 시스템을 학습하는 프로젝트

### 핵심 학습 포인트
- ✅ **완전한 Non-Blocking Stack** (WebFlux + R2DBC + Reactive Redis)
- ✅ **함수형 프로그래밍** (Mono/Flux 리액티브 스트림)
- ✅ **이벤트 루프 기반 고성능 처리** (Netty)
- ✅ **복잡한 도메인 모델 설계** (DDD)
- ✅ **동시성 제어** (Reactive 재고 관리)
- ✅ **JWT 기반 인증** (Stateless Authentication)
- ✅ **완전한 테스트 커버리지** (성공 1개 + 실패 5개 전략)

### 기술 스택

| 분류 | 기술 | 비고 |
|------|------|------|
| **Language** | Java 17 | |
| **Framework** | Spring Boot 3.2.0 | |
| **Reactive** | Spring WebFlux | **NOT Spring MVC** |
| **Web Server** | Netty | 비동기 이벤트 루프 |
| **Database Access** | Spring Data R2DBC | **NOT JPA/Hibernate** |
| **Database** | MySQL 8.0 (R2DBC Driver) | |
| **Migration** | Flyway | JDBC 기반 (마이그레이션 전용) |
| **Cache** | Redis (Reactive) | Refresh Token 저장 |
| **Security** | Spring Security (WebFlux) | JWT 기반 |
| **Build** | Gradle 8.3 | |
| **Test** | JUnit 5, Mockito, Reactor Test | StepVerifier 활용 |
| **Test DB** | H2 (R2DBC) | |
| **API Docs** | Spring REST Docs + Asciidoctor | |

---

## 🏗️ 시스템 아키텍처

### 리액티브 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    Client (Web/App)                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Netty (Event Loop)                         │
│              비동기 논블로킹 웹 서버 (8개 스레드)                  │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring WebFlux Application                      │
│                                                              │
│  Controller (Mono/Flux) → Service (Reactive Chains)          │
│                          → Repository (R2DBC)                │
│                                                              │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐            │
│  │ Member  │ │ Product │ │  Order  │ │ Payment │  ...       │
│  │ Domain  │ │ Domain  │ │ Domain  │ │ Domain  │            │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘            │
└─────────────────────────┬───────────────────────────────────┘
                          │
              ┌───────────┼───────────┐
              ▼           ▼           ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │  MySQL   │ │  Redis   │ │   S3     │
        │ (R2DBC)  │ │(Reactive)│ │ (Image)  │
        └──────────┘ └──────────┘ └──────────┘
```

### Why Reactive?

#### 기존 방식 (Spring MVC + JPA)
```
Thread per Request
요청 10,000개 = 스레드 10,000개 필요 (불가능)
→ Thread Pool 200개 → 나머지 9,800개 대기
```

#### 리액티브 방식 (Spring WebFlux + R2DBC)
```
Event Loop (Netty)
요청 10,000개 = 스레드 8개로 처리 가능
→ I/O 대기 중 다른 요청 처리
→ 동시 처리 능력 50배 향상
```

---

## 📦 도메인 분석

### 구현 완료 ✅

| 도메인 | 설명 | 주요 기능 | 상태 |
|--------|------|----------|------|
| **Member** | 회원 관리 | 회원가입, 로그인, 등급, 적립금 | ✅ 완료 |
| **Auth** | 인증/인가 | JWT (Access + Refresh), Redis Session | ✅ 완료 |
| **Address** | 배송지 관리 | 배송지 CRUD, 기본 배송지 설정 | ✅ 완료 |

### 구현 예정 ⏳

| 도메인 | 설명 | 복잡도 | Phase |
|--------|------|--------|-------|
| **Product** | 상품 등록, 옵션 조합, 카테고리 | ⭐⭐⭐⭐ | 2 |
| **Inventory** | 재고 관리, 동시성 제어 | ⭐⭐⭐⭐⭐ | 2 |
| **Cart** | 장바구니, 판매자별 묶음 | ⭐⭐⭐ | 2 |
| **Order** | 주문 생성, 상태 머신, 부분 취소 | ⭐⭐⭐⭐⭐ | 3 |
| **Payment** | 결제 처리, 복합 결제 | ⭐⭐⭐⭐ | 3 |
| **Delivery** | 배송 상태 관리, 배송비 계산 | ⭐⭐⭐⭐ | 3 |
| **Coupon** | 쿠폰 발급/사용, 중복 적용 규칙 | ⭐⭐⭐⭐⭐ | 4 |
| **Review** | 리뷰 작성, 평점 집계 | ⭐⭐⭐ | 4 |
| **Seller** | 판매자 관리, 정산 | ⭐⭐⭐⭐⭐ | 5 |

---

## ✅ 구현 완료 도메인 상세

### 1. Member Domain

#### 엔티티 설계 (R2DBC)

```java
@Table("members")
public class Member extends BaseEntity {
    @Id
    private Long id;

    @Column("email")
    private String email;

    @Column("password")
    private String password;  // BCrypt 암호화

    @Column("name")
    private String name;

    @Column("phone")
    private String phone;

    @Column("grade")
    private MemberGrade grade;  // BRONZE, SILVER, GOLD, VIP

    @Column("point")
    private Integer point;  // 적립금

    @Column("status")
    private MemberStatus status;  // ACTIVE, DORMANT, WITHDRAWN, SUSPENDED

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    // 로켓와우 (구독 서비스)
    @Column("rocket_wow_active")
    private Boolean rocketWowActive;

    @Column("rocket_wow_expires_at")
    private LocalDateTime rocketWowExpiresAt;

    // 비즈니스 로직 메서드
    public Member login() {
        this.lastLoginAt = LocalDateTime.now();
        return this;
    }

    public Member usePoint(int amount) {
        if (this.point < amount) {
            throw new IllegalStateException("적립금이 부족합니다.");
        }
        this.point -= amount;
        return this;
    }

    public Member upgradeGrade(MemberGrade newGrade) {
        this.grade = newGrade;
        return this;
    }
}

public enum MemberGrade {
    BRONZE(1, 0, 0.01, "브론즈"),
    SILVER(2, 100_000, 0.02, "실버"),
    GOLD(3, 500_000, 0.03, "골드"),
    VIP(4, 1_000_000, 0.05, "VIP");

    // 함수형 메서드
    public static MemberGrade calculateGrade(int totalPurchaseAmount) {
        return Arrays.stream(values())
            .filter(grade -> totalPurchaseAmount >= grade.threshold)
            .reduce((first, second) -> second)
            .orElse(BRONZE);
    }
}
```

#### Repository (Reactive)

```java
public interface MemberRepository extends ReactiveCrudRepository<Member, Long> {
    Mono<Member> findByEmail(String email);
    Mono<Boolean> existsByEmail(String email);

    @Query("SELECT * FROM members WHERE status = :status AND last_login_at < :dormantDate")
    Flux<Member> findDormantMembers(@Param("status") MemberStatus status,
                                    @Param("dormantDate") LocalDateTime dormantDate);
}
```

#### Service (함수형 리액티브)

```java
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

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

    // Blocking 작업은 별도 스레드풀에서 실행
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
            .subscribeOn(Schedulers.boundedElastic());  // BCrypt는 블로킹
    }
}
```

#### 비즈니스 규칙
- 이메일, 전화번호 중복 불가
- 회원 등급: 구매 금액에 따라 자동 업그레이드
- 적립금: 최소 1,000원 이상 사용 가능
- 로켓와우: 월 구독 서비스, 무료배송 혜택
- 휴면 전환: 1년 미접속 시 자동 휴면

---

### 2. Auth Domain (JWT)

#### AuthService (Reactive)

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Transactional
    public Mono<TokenResponse> login(LoginRequest request) {
        return memberRepository.findByEmail(request.getEmail())
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_CREDENTIALS)))
            .filterWhen(member -> validatePassword(request.getPassword(), member.getPassword()))
            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_PASSWORD)))
            .flatMap(this::validateMemberStatus)
            .flatMap(this::updateLastLogin)
            .flatMap(this::generateTokens)
            .doOnSuccess(token -> log.info("로그인 성공: {}", request.getEmail()));
    }

    // JWT 토큰 생성 및 Redis 저장
    private Mono<TokenResponse> generateTokens(Member member) {
        return Mono.fromCallable(() -> {
                String accessToken = jwtTokenProvider.createAccessToken(member.getId());
                String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
                return TokenResponse.of(accessToken, refreshToken);
            })
            .flatMap(tokenResponse ->
                saveRefreshToken(member.getId(), tokenResponse.getRefreshToken())
                    .thenReturn(tokenResponse)
            );
    }
}
```

#### 인증 흐름
1. **로그인**: 이메일/비밀번호 검증 → JWT 발급 (Access + Refresh)
2. **Access Token**: HTTP 헤더 `Authorization: Bearer {token}`
3. **Refresh Token**: Redis 저장 (TTL: 7일)
4. **토큰 갱신**: Refresh Token으로 새 Access Token 발급

---

### 3. Address Domain

#### 엔티티

```java
@Table("addresses")
public class Address extends BaseEntity {
    @Id
    private Long id;

    @Column("member_id")
    private Long memberId;

    private String name;        // 배송지명 (집, 회사)
    private String recipient;   // 수령인
    private String phone;
    private String zipCode;
    private String address;
    private String addressDetail;

    @Column("is_default")
    private Boolean isDefault;  // 기본 배송지 여부

    private String deliveryRequest;  // 배송 요청사항
}
```

#### 비즈니스 규칙
- 최대 배송지 개수: 10개
- 첫 번째 배송지: 자동으로 기본 배송지 설정
- 기본 배송지 삭제: 다른 배송지가 있으면 불가
- 기본 배송지 변경: 기존 기본 배송지 자동 해제

---

## 📡 API 설계

### Auth API

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| POST | `/api/v1/auth/signup` | 회원가입 | ✅ |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) | ✅ |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 | ✅ |
| POST | `/api/v1/auth/logout` | 로그아웃 (Redis 토큰 삭제) | ✅ |

### Member API

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| GET | `/api/v1/members/me` | 내 정보 조회 | ✅ |
| PUT | `/api/v1/members/me` | 내 정보 수정 | ✅ |
| DELETE | `/api/v1/members/me` | 회원 탈퇴 | ✅ |
| POST | `/api/v1/members/me/rocket-wow` | 로켓와우 구독 | ✅ |
| DELETE | `/api/v1/members/me/rocket-wow` | 로켓와우 취소 | ✅ |

### Address API

| Method | Endpoint | 설명 | 상태 |
|--------|----------|------|------|
| GET | `/api/v1/members/me/addresses` | 배송지 목록 | ✅ |
| GET | `/api/v1/members/me/addresses/default` | 기본 배송지 조회 | ✅ |
| POST | `/api/v1/members/me/addresses` | 배송지 추가 | ✅ |
| GET | `/api/v1/members/me/addresses/{id}` | 배송지 조회 | ✅ |
| PUT | `/api/v1/members/me/addresses/{id}/default` | 기본 배송지 설정 | ✅ |
| DELETE | `/api/v1/members/me/addresses/{id}` | 배송지 삭제 | ✅ |

---

## 🧪 테스트 전략

### 테스트 구조

```
Controller Test (WebFluxTest)
  - WebTestClient로 HTTP 요청 테스트
  - REST Docs 자동 문서화

Service Test (MockitoExtension)
  - Reactor Test (StepVerifier)
  - 비즈니스 로직 검증
```

### 테스트 커버리지 규칙

**각 API당 성공 1개 + 실패 5개 = 총 6개 테스트**

```java
// Controller Test 예시
@WebFluxTest(AuthController.class)
@AutoConfigureRestDocs
class AuthControllerTest {

    @Test
    @DisplayName("[성공] 회원가입")
    void signup_success() {
        webTestClient.post()
            .uri("/api/v1/auth/signup")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .consumeWith(document("auth/signup-success",
                requestFields(...),
                responseFields(...)
            ));
    }

    @Test void signup_fail_duplicateEmail() { ... }
    @Test void signup_fail_duplicatePhone() { ... }
    @Test void signup_fail_invalidEmailFormat() { ... }
    @Test void signup_fail_invalidPasswordFormat() { ... }
    @Test void signup_fail_missingRequiredField() { ... }
}
```

### Service Test (Reactive)

```java
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Test
    void signup_success() {
        // given
        given(memberRepository.save(any()))
            .willReturn(Mono.just(member));

        // when
        Mono<MemberResponse> result = memberService.signup(request);

        // then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response.getEmail()).isEqualTo("test@example.com");
            })
            .verifyComplete();
    }
}
```

### 테스트 결과

- **AuthController**: 10개 테스트 (signup 6개 + login 4개)
- **MemberService**: 30개 테스트
- **AddressService**: 28개 테스트
- **MemberController**: 20개 테스트

**총 88개 테스트 작성 완료**

---

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/com/example/ecommerce/
│   │   ├── domain/
│   │   │   └── member/
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   └── MemberController.java
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   │   ├── SignupRequest.java
│   │   │       │   │   ├── LoginRequest.java
│   │   │       │   │   ├── MemberUpdateRequest.java
│   │   │       │   │   └── AddressCreateRequest.java
│   │   │       │   └── response/
│   │   │       │       ├── MemberResponse.java
│   │   │       │       ├── TokenResponse.java
│   │   │       │       └── AddressResponse.java
│   │   │       ├── entity/
│   │   │       │   ├── Member.java
│   │   │       │   ├── MemberGrade.java
│   │   │       │   ├── MemberStatus.java
│   │   │       │   └── Address.java
│   │   │       ├── repository/
│   │   │       │   ├── MemberRepository.java
│   │   │       │   └── AddressRepository.java
│   │   │       └── service/
│   │   │           ├── MemberService.java
│   │   │           ├── AuthService.java
│   │   │           └── AddressService.java
│   │   │
│   │   └── global/
│   │       ├── auth/
│   │       │   ├── JwtTokenProvider.java
│   │       │   └── SecurityConfig.java
│   │       ├── common/
│   │       │   ├── BaseEntity.java
│   │       │   └── ApiResponse.java
│   │       ├── config/
│   │       │   ├── R2dbcConfig.java
│   │       │   └── RedisConfig.java
│   │       └── exception/
│   │           ├── GlobalExceptionHandler.java
│   │           ├── BusinessException.java
│   │           └── ErrorCode.java
│   │
│   └── resources/
│       ├── application.yml
│       └── db/migration/
│           └── V1__init_schema.sql
│
└── test/
    └── java/com/example/ecommerce/
        └── domain/member/
            ├── controller/
            │   ├── AuthControllerTest.java
            │   └── MemberControllerTest.java
            └── service/
                ├── MemberServiceTest.java
                └── AddressServiceTest.java
```

---

## ⚙️ 설정 파일

### application.yml

```yaml
spring:
  application:
    name: ecommerce

  # R2DBC (Reactive Database)
  r2dbc:
    url: r2dbc:mysql://localhost:3306/ecommerce
    username: root
    password: password

  # Redis (Reactive)
  data:
    redis:
      host: localhost
      port: 6379

  # Flyway (마이그레이션)
  flyway:
    enabled: true
    url: jdbc:mysql://localhost:3306/ecommerce
    user: root
    password: password

# JWT 설정
jwt:
  secret: ${JWT_SECRET:your-secret-key-at-least-256-bits-long}
  access-token-expiration: 3600000   # 1시간
  refresh-token-expiration: 604800000 # 7일

# Server (Netty)
server:
  port: 8080

# Logging
logging:
  level:
    org.springframework.r2dbc: DEBUG
    io.r2dbc.proxy: DEBUG
```

### build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'org.asciidoctor.jvm.convert' version '3.3.2'
}

dependencies {
    // Spring Boot WebFlux
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // Redis (Reactive)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'

    // R2DBC MySQL
    implementation 'io.asyncer:r2dbc-mysql:1.0.5'

    // JDBC (Flyway 전용)
    runtimeOnly 'com.mysql:mysql-connector-j'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'

    // Swagger (WebFlux용)
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'io.r2dbc:r2dbc-h2'

    // REST Docs
    testImplementation 'org.springframework.restdocs:spring-restdocs-webtestclient'
    asciidoctorExt 'org.springframework.restdocs:spring-restdocs-asciidoctor'
}

test {
    useJUnitPlatform()
    outputs.dir snippetsDir
}

asciidoctor {
    inputs.dir snippetsDir
    configurations 'asciidoctorExt'
    dependsOn test
}
```

---

## 🚀 실행 방법

### 1. Docker로 인프라 실행

```bash
# MySQL + Redis 실행
docker-compose up -d

# 확인
docker ps
```

### 2. 데이터베이스 마이그레이션

```bash
# Flyway가 자동으로 실행됨 (애플리케이션 시작 시)
```

### 3. 애플리케이션 실행

```bash
# 환경변수 설정
export JWT_SECRET=your-secret-key-at-least-256-bits-long

# 실행
./gradlew bootRun
```

### 4. 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 패키지 테스트
./gradlew test --tests "com.example.ecommerce.domain.member.*"

# REST Docs 생성
./gradlew asciidoctor
```

### 5. API 문서 확인

```
# Swagger UI
http://localhost:8080/swagger-ui.html

# REST Docs (테스트 실행 후)
build/docs/asciidoc/index.html
```

---

## 📊 성능 비교

### Spring MVC (Tomcat) vs WebFlux (Netty)

| 항목 | Spring MVC | Spring WebFlux |
|------|------------|----------------|
| **웹 서버** | Tomcat | Netty |
| **처리 방식** | Thread per Request | Event Loop |
| **필요 스레드** | 요청 수만큼 | CPU 코어 수 (8개) |
| **동시 접속** | 200~500명 | 10,000명+ |
| **메모리 사용** | 높음 (스레드 스택) | 낮음 |
| **DB 접근** | JPA (블로킹) | R2DBC (논블로킹) |
| **반환 타입** | `List`, `Optional` | `Flux`, `Mono` |

### 벤치마크 예상

```
동시 사용자 10,000명 기준

Tomcat (Thread Pool 200):
- 처리량: 200 req/sec
- 평균 응답: 50초

Netty (Event Loop 8):
- 처리량: 10,000 req/sec
- 평균 응답: 1초
```

---

## 🗓️ 개발 로드맵

### ✅ Phase 1: Member Domain (완료)

- [x] 프로젝트 셋업 (WebFlux + R2DBC)
- [x] Member 엔티티 및 Repository
- [x] Auth Service (JWT + Redis)
- [x] Address Service
- [x] Controller 구현
- [x] 테스트 코드 (88개)
- [x] REST Docs 설정

### ⏳ Phase 2: Product Domain (예정)

- [ ] Product 엔티티 (상품, 옵션, 카테고리)
- [ ] Product Repository (R2DBC)
- [ ] Product Service (Reactive)
- [ ] 상품 목록/상세 API
- [ ] 테스트 코드

### ⏳ Phase 3: Order Domain (예정)

- [ ] Order 엔티티 (주문, 주문 아이템)
- [ ] Order 상태 머신
- [ ] Inventory 동시성 제어 (Reactive)
- [ ] Payment 연동 (Mock)
- [ ] 테스트 코드

### ⏳ Phase 4: 확장 기능 (예정)

- [ ] Coupon System
- [ ] Review System
- [ ] Seller & Settlement
- [ ] Elasticsearch 검색

---

## 📚 학습 자료

### 리액티브 프로그래밍
- [Project Reactor 공식 문서](https://projectreactor.io/docs)
- [Spring WebFlux 레퍼런스](https://docs.spring.io/spring-framework/reference/web/webflux.html)

### R2DBC
- [R2DBC 공식 문서](https://r2dbc.io/)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)

### Netty
- [Netty 공식 문서](https://netty.io/wiki/)

### 기타
- [쿠팡 기술 블로그](https://medium.com/coupang-engineering)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

## 📝 참고 문서

- **프로젝트 계획**: `/plan/` 디렉토리 (6개 파일, 185KB)
- **완료 내역**: `done.md`
- **Claude 컨텍스트**: `claude.md`

---

**Made with ❤️ for Reactive E-commerce Systems**
