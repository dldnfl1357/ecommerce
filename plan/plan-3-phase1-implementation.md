# 쿠팡 클론 프로젝트 기획서 (3/6) - Phase 1 구현 가이드

> **기반 구축 및 핵심 구매 플로우 구현 (Week 1-8)**

---

## 📋 목차
1. [Week 1-2: 프로젝트 셋업](#week-1-2-프로젝트-셋업)
2. [Week 3-4: Member Service](#week-3-4-member-service)
3. [Week 5-6: Product Service](#week-5-6-product-service)
4. [Week 7-8: Inventory Service](#week-7-8-inventory-service)

---

## 🚀 Week 1-2: 프로젝트 셋업

### Day 1-2: 프로젝트 초기화 및 인프라 설정

#### 1.1 프로젝트 구조 생성

```bash
# 1. 부모 프로젝트 생성
mkdir coupang-clone
cd coupang-clone

# 2. 마이크로서비스 구조 생성
mkdir -p services/{member-service,product-service,order-service,payment-service}
mkdir -p infrastructure/{gateway,config-server,discovery-server}
mkdir -p common/{common-core,common-security,common-web}
```

**디렉토리 구조**:
```
coupang-clone/
├── services/
│   ├── member-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── delivery-service/
│   ├── coupon-service/
│   ├── search-service/
│   └── seller-service/
├── infrastructure/
│   ├── api-gateway/
│   ├── config-server/
│   └── discovery-server/
├── common/
│   ├── common-core/         # 공통 엔티티, Exception
│   ├── common-security/     # JWT, OAuth
│   └── common-web/          # ApiResponse, GlobalException
├── docker/
│   ├── mysql/
│   ├── redis/
│   ├── kafka/
│   └── elasticsearch/
└── k8s/
    ├── base/
    └── overlays/
```

#### 1.2 공통 모듈 설정 (common-core)

**build.gradle**:
```groovy
// common-core/build.gradle
plugins {
    id 'java-library'
    id 'org.springframework.boot' version '3.2.0' apply false
    id 'io.spring.dependency-management' version '1.1.4'
}

dependencies {
    api 'org.springframework.boot:spring-boot-starter-data-jpa'
    api 'org.springframework.boot:spring-boot-starter-validation'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

**BaseEntity.java**:
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false, length = 50)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 50)
    private String updatedBy;
}
```

**ErrorCode.java**:
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(400, "C001", "Invalid Input Value"),
    METHOD_NOT_ALLOWED(405, "C002", "Method Not Allowed"),
    INTERNAL_SERVER_ERROR(500, "C003", "Internal Server Error"),

    // Member
    MEMBER_NOT_FOUND(404, "M001", "Member Not Found"),
    DUPLICATE_EMAIL(400, "M002", "Email Already Exists"),
    INVALID_PASSWORD(400, "M003", "Invalid Password"),

    // Product
    PRODUCT_NOT_FOUND(404, "P001", "Product Not Found"),
    PRODUCT_OPTION_NOT_FOUND(404, "P002", "Product Option Not Found"),

    // Inventory
    INSUFFICIENT_STOCK(400, "I001", "Insufficient Stock"),

    // Order
    ORDER_NOT_FOUND(404, "O001", "Order Not Found"),
    ORDER_NOT_CANCELLABLE(400, "O002", "Order Not Cancellable"),

    // Payment
    PAYMENT_FAILED(400, "PAY001", "Payment Failed"),
    PAYMENT_NOT_FOUND(404, "PAY002", "Payment Not Found");

    private final int status;
    private final String code;
    private final String message;
}
```

#### 1.3 API Gateway 설정

**build.gradle**:
```groovy
// api-gateway/build.gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
}
```

**application.yml**:
```yaml
server:
  port: 8000

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        # Member Service
        - id: member-service
          uri: lb://MEMBER-SERVICE
          predicates:
            - Path=/api/v1/members/**
          filters:
            - RewritePath=/api/v1/members/(?<segment>.*), /${segment}
            - name: CircuitBreaker
              args:
                name: memberCircuitBreaker
                fallbackUri: forward:/fallback/members

        # Product Service
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/v1/products/**
          filters:
            - RewritePath=/api/v1/products/(?<segment>.*), /${segment}

        # Order Service
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - RewritePath=/api/v1/orders/(?<segment>.*), /${segment}

      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"
              - "https://coupang.example.com"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600

      default-filters:
        - name: Retry
          args:
            retries: 3
            statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 10
            redis-rate-limiter.burstCapacity: 20

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

#### 1.4 Service Discovery (Eureka)

**application.yml**:
```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-server

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

### Day 3-4: Docker & Kubernetes 설정

#### 2.1 Docker Compose (로컬 개발)

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: coupang-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: coupang
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./docker/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7-alpine
    container_name: coupang-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: coupang-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: coupang-zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: coupang-elasticsearch
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - es-data:/usr/share/elasticsearch/data

  prometheus:
    image: prom/prometheus:latest
    container_name: coupang-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus

  grafana:
    image: grafana/grafana:latest
    container_name: coupang-grafana
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana

volumes:
  mysql-data:
  redis-data:
  es-data:
  prometheus-data:
  grafana-data:
```

#### 2.2 CI/CD 파이프라인 (GitHub Actions)

**.github/workflows/ci.yml**:
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run tests
        run: ./gradlew test

      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results
          path: '**/build/test-results/test/*.xml'

      - name: Code coverage
        run: ./gradlew jacocoTestReport

      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: ./gradlew sonarqube

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew bootJar

      - name: Docker Login
        uses: docker/login-action@v2
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push Docker image
        run: |
          docker build -t ghcr.io/${{ github.repository }}/member-service:${{ github.sha }} \
            ./services/member-service
          docker push ghcr.io/${{ github.repository }}/member-service:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3

      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/member-service \
            member-service=ghcr.io/${{ github.repository }}/member-service:${{ github.sha }} \
            -n production
```

---

## 👤 Week 3-4: Member Service

### Day 1: 프로젝트 구조 및 엔티티 구현

#### 3.1 프로젝트 구조

```
member-service/
├── src/
│   ├── main/
│   │   ├── java/com/coupang/member/
│   │   │   ├── MemberServiceApplication.java
│   │   │   ├── domain/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Member.java
│   │   │   │   │   ├── Address.java
│   │   │   │   │   └── MemberGrade.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── MemberRepository.java
│   │   │   │   │   └── AddressRepository.java
│   │   │   │   └── service/
│   │   │   │       ├── MemberService.java
│   │   │   │       ├── AuthService.java
│   │   │   │       └── AddressService.java
│   │   │   ├── api/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── MemberController.java
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   └── AddressController.java
│   │   │   │   └── dto/
│   │   │   │       ├── request/
│   │   │   │       │   ├── SignupRequest.java
│   │   │   │       │   ├── LoginRequest.java
│   │   │   │       │   └── AddressCreateRequest.java
│   │   │   │       └── response/
│   │   │   │           ├── MemberResponse.java
│   │   │   │           ├── TokenResponse.java
│   │   │   │           └── AddressResponse.java
│   │   │   ├── infrastructure/
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   └── event/
│   │   │   │       ├── MemberEventPublisher.java
│   │   │   │       └── MemberEventHandler.java
│   │   │   └── config/
│   │   │       ├── JpaConfig.java
│   │   │       └── KafkaConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__init_member_schema.sql
│   └── test/
│       └── java/com/coupang/member/
│           ├── domain/service/
│           ├── api/controller/
│           └── integration/
└── build.gradle
```

#### 3.2 Member Service 구현

**MemberService.java**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberEventPublisher eventPublisher;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        // 1. 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        // 2. 회원 생성
        Member member = Member.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .phone(request.getPhone())
            .grade(MemberGrade.BRONZE)
            .status(MemberStatus.ACTIVE)
            .build();

        Member savedMember = memberRepository.save(member);

        // 3. 이벤트 발행
        eventPublisher.publishMemberCreated(savedMember);

        return MemberResponse.from(savedMember);
    }

    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException(memberId));

        return MemberResponse.from(member);
    }

    @Transactional
    public void updateGrade(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException(memberId));

        // 최근 6개월 구매 금액 조회
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        Integer totalPurchase = memberRepository
            .calculateTotalPurchaseAmount(memberId, sixMonthsAgo);

        // 등급 계산 및 업데이트
        MemberGrade newGrade = MemberGrade.calculateGrade(totalPurchase);
        if (newGrade != member.getGrade()) {
            member.upgradeGrade(newGrade);
            eventPublisher.publishMemberGradeChanged(member, newGrade);
        }
    }

    @Transactional
    public void usePoint(Long memberId, int amount) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.usePoint(amount);
    }

    @Transactional
    public void earnPoint(Long memberId, int amount) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException(memberId));

        member.earnPoint(amount);
    }
}
```

**AuthService.java**:
```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenResponse login(LoginRequest request) {
        // 1. 회원 조회
        Member member = memberRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new InvalidCredentialsException());

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // 3. 휴면/탈퇴 회원 체크
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new InactiveMemberException(member.getStatus());
        }

        // 4. 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // 5. Refresh Token 저장
        refreshTokenRepository.save(
            RefreshToken.of(member.getId(), refreshToken)
        );

        // 6. 로그인 시간 업데이트
        member.login();

        return TokenResponse.of(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        // 2. Refresh Token 조회
        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        RefreshToken savedToken = refreshTokenRepository.findByMemberId(memberId)
            .orElseThrow(() -> new InvalidTokenException());

        if (!savedToken.getToken().equals(refreshToken)) {
            throw new InvalidTokenException();
        }

        // 3. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(memberId);

        return TokenResponse.of(newAccessToken, refreshToken);
    }

    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}
```

### Day 2: API Controller 구현

**MemberController.java**:
```java
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyInfo(
        @AuthenticationPrincipal Long memberId
    ) {
        MemberResponse response = memberService.getMember(memberId);
        return ApiResponse.success(response);
    }

    @PutMapping("/me")
    public ApiResponse<MemberResponse> updateMyInfo(
        @AuthenticationPrincipal Long memberId,
        @Valid @RequestBody MemberUpdateRequest request
    ) {
        MemberResponse response = memberService.updateMember(memberId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(
        @AuthenticationPrincipal Long memberId
    ) {
        memberService.withdraw(memberId);
        return ApiResponse.success();
    }
}

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;

    @PostMapping("/signup")
    public ApiResponse<MemberResponse> signup(
        @Valid @RequestBody SignupRequest request
    ) {
        MemberResponse response = memberService.signup(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
        @RequestHeader("Refresh-Token") String refreshToken
    ) {
        TokenResponse response = authService.refresh(refreshToken);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
        @AuthenticationPrincipal Long memberId
    ) {
        authService.logout(memberId);
        return ApiResponse.success();
    }
}
```

### Day 3-4: JWT 인증 구현

**JwtTokenProvider.java**:
```java
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long memberId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
            .setSubject(String.valueOf(memberId))
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
            .setSubject(String.valueOf(memberId))
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getMemberId(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();

        return Long.parseLong(claims.getSubject());
    }
}
```

**JwtAuthenticationFilter.java**:
```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long memberId = jwtTokenProvider.getMemberId(token);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                memberId, null, Collections.emptyList()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
```

### Day 5: 테스트 코드 작성

**MemberServiceTest.java**:
```java
@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 가입 성공")
    void signup_success() {
        // given
        SignupRequest request = SignupRequest.builder()
            .email("test@example.com")
            .password("password123")
            .name("홍길동")
            .phone("010-1234-5678")
            .build();

        // when
        MemberResponse response = memberService.signup(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getGrade()).isEqualTo(MemberGrade.BRONZE);
    }

    @Test
    @DisplayName("중복 이메일로 회원 가입 시 예외 발생")
    void signup_duplicate_email() {
        // given
        memberRepository.save(Member.builder()
            .email("test@example.com")
            .password("password")
            .name("기존회원")
            .phone("010-0000-0000")
            .build());

        SignupRequest request = SignupRequest.builder()
            .email("test@example.com")
            .password("password123")
            .name("신규회원")
            .phone("010-1111-1111")
            .build();

        // when & then
        assertThatThrownBy(() -> memberService.signup(request))
            .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("회원 등급 업그레이드")
    void upgrade_member_grade() {
        // given
        Member member = memberRepository.save(Member.builder()
            .email("test@example.com")
            .password("password")
            .name("홍길동")
            .phone("010-1234-5678")
            .grade(MemberGrade.BRONZE)
            .build());

        // 50만원 구매 이력 생성 (Mocking)
        when(memberRepository.calculateTotalPurchaseAmount(any(), any()))
            .thenReturn(500_000);

        // when
        memberService.updateGrade(member.getId());

        // then
        Member updated = memberRepository.findById(member.getId()).get();
        assertThat(updated.getGrade()).isEqualTo(MemberGrade.GOLD);
    }
}
```

---

## 📦 Week 5-6: Product Service

### Day 1-2: Product 도메인 구현

**ProductService.java**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository;
    private final S3FileUploader s3FileUploader;
    private final ProductEventPublisher eventPublisher;

    @Transactional
    public ProductResponse createProduct(
        Long sellerId,
        ProductCreateRequest request
    ) {
        // 1. 판매자 조회
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new SellerNotFoundException(sellerId));

        // 2. 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        // 3. 상품 생성
        Product product = Product.builder()
            .seller(seller)
            .category(category)
            .name(request.getName())
            .description(request.getDescription())
            .basePrice(request.getBasePrice())
            .deliveryType(request.getDeliveryType())
            .status(ProductStatus.STOP_SALE)
            .build();

        // 4. 상품 옵션 추가
        for (ProductOptionRequest optionReq : request.getOptions()) {
            ProductOption option = ProductOption.builder()
                .product(product)
                .optionName(optionReq.getOptionName())
                .option1(optionReq.getOption1())
                .option2(optionReq.getOption2())
                .addPrice(optionReq.getAddPrice())
                .build();

            product.addOption(option);

            // 재고 생성
            Inventory inventory = Inventory.create(option, optionReq.getInitialStock());
            option.setInventory(inventory);
        }

        // 5. 이미지 업로드 및 저장
        for (MultipartFile image : request.getImages()) {
            String imageUrl = s3FileUploader.upload(image, "products");

            ProductImage productImage = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .sortOrder(request.getImages().indexOf(image))
                .isThumbnail(request.getImages().indexOf(image) == 0)
                .type(ImageType.MAIN)
                .build();

            product.addImage(productImage);
        }

        Product savedProduct = productRepository.save(product);

        // 6. 이벤트 발행 (검색 인덱싱)
        eventPublisher.publishProductCreated(savedProduct);

        return ProductResponse.from(savedProduct);
    }

    public ProductDetailResponse getProduct(Long productId) {
        Product product = productRepository.findByIdWithDetails(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        // 조회수 증가 (비동기)
        product.increaseViewCount();

        return ProductDetailResponse.from(product);
    }

    public Page<ProductResponse> searchProducts(ProductSearchCondition condition, Pageable pageable) {
        return productRepository.search(condition, pageable);
    }
}
```

**ProductRepository.java** (QueryDSL):
```java
public interface ProductRepositoryCustom {
    Page<ProductResponse> search(ProductSearchCondition condition, Pageable pageable);
    Optional<Product> findByIdWithDetails(Long productId);
}

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProductResponse> search(
        ProductSearchCondition condition,
        Pageable pageable
    ) {
        List<Product> products = queryFactory
            .selectFrom(product)
            .leftJoin(product.category, category).fetchJoin()
            .leftJoin(product.seller, seller).fetchJoin()
            .where(
                categoryIdEq(condition.getCategoryId()),
                keywordContains(condition.getKeyword()),
                priceGoe(condition.getMinPrice()),
                priceLoe(condition.getMaxPrice()),
                statusEq(ProductStatus.ON_SALE)
            )
            .orderBy(getOrderSpecifier(condition.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(product.count())
            .from(product)
            .where(
                categoryIdEq(condition.getCategoryId()),
                keywordContains(condition.getKeyword()),
                priceGoe(condition.getMinPrice()),
                priceLoe(condition.getMaxPrice()),
                statusEq(ProductStatus.ON_SALE)
            )
            .fetchOne();

        List<ProductResponse> responses = products.stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
    }

    private BooleanExpression categoryIdEq(Long categoryId) {
        return categoryId != null ? product.category.id.eq(categoryId) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ?
            product.name.contains(keyword)
                .or(product.description.contains(keyword)) : null;
    }

    private OrderSpecifier<?> getOrderSpecifier(ProductSort sort) {
        return switch (sort) {
            case LATEST -> product.createdAt.desc();
            case PRICE_ASC -> product.basePrice.asc();
            case PRICE_DESC -> product.basePrice.desc();
            case POPULAR -> product.salesCount.desc();
            case RATING -> product.averageRating.desc();
        };
    }
}
```

---

## 📊 Week 7-8: Inventory Service

### Day 1-3: 재고 관리 및 동시성 제어

**InventoryService.java**:
```java
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RedissonClient redissonClient;

    /**
     * Redis 분산 락을 이용한 재고 예약
     */
    @Transactional
    public void reserve(Long productOptionId, int quantity) {
        String lockKey = "inventory:lock:" + productOptionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (대기 10초, 점유 5초)
            boolean acquired = lock.tryLock(10, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new LockAcquisitionException("재고 락 획득 실패");
            }

            // 재고 조회
            Inventory inventory = inventoryRepository
                .findByProductOptionId(productOptionId)
                .orElseThrow(() -> new InventoryNotFoundException(productOptionId));

            // 재고 예약
            inventory.reserve(quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재고 예약 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public void confirmReservation(Long productOptionId, int quantity) {
        Inventory inventory = inventoryRepository
            .findByProductOptionId(productOptionId)
            .orElseThrow(() -> new InventoryNotFoundException(productOptionId));

        inventory.confirmReservation(quantity);
    }

    @Transactional
    public void cancelReservation(Long productOptionId, int quantity) {
        Inventory inventory = inventoryRepository
            .findByProductOptionId(productOptionId)
            .orElseThrow(() -> new InventoryNotFoundException(productOptionId));

        inventory.cancelReservation(quantity);
    }

    @Transactional
    public void increase(Long productOptionId, int quantity) {
        String lockKey = "inventory:lock:" + productOptionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            lock.lock(5, TimeUnit.SECONDS);

            Inventory inventory = inventoryRepository
                .findByProductOptionId(productOptionId)
                .orElseThrow(() -> new InventoryNotFoundException(productOptionId));

            inventory.increase(quantity);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### Day 4-5: 동시성 테스트

**InventoryConcurrencyTest.java**:
```java
@SpringBootTest
class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("100명이 동시에 재고 10개를 주문하면 10건만 성공")
    void concurrentInventoryReservation() throws InterruptedException {
        // given
        Long productOptionId = 1L;
        Inventory inventory = Inventory.create(productOption, 10);
        inventoryRepository.save(inventory);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    inventoryService.reserve(productOptionId, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(90);

        Inventory result = inventoryRepository.findById(inventory.getId()).get();
        assertThat(result.getReservedQuantity()).isEqualTo(10);
    }
}
```

**다음 파일**: plan-4-phase2-implementation.md에서 주문/결제/배송 구현을 다룹니다.
