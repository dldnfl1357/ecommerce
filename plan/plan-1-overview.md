# 쿠팡 클론 프로젝트 기획서 (1/6) - 프로젝트 개요

> **대규모 트래픽 처리가 가능한 차세대 이커머스 플랫폼**

---

## 📋 목차
1. [프로젝트 비전](#프로젝트-비전)
2. [핵심 목표](#핵심-목표)
3. [비즈니스 요구사항](#비즈니스-요구사항)
4. [시스템 아키텍처](#시스템-아키텍처)
5. [기술 스택](#기술-스택)
6. [개발 전략](#개발-전략)
7. [프로젝트 타임라인](#프로젝트-타임라인)

---

## 🎯 프로젝트 비전

### 핵심 비전
**"한국 최고의 이커머스 플랫폼인 쿠팡의 핵심 기능을 학습하고, 대규모 트래픽을 처리할 수 있는 확장 가능한 시스템 구축"**

### 프로젝트 목적
1. **대규모 트래픽 처리 경험**: 수백만 동시 사용자를 처리할 수 있는 시스템 설계
2. **복잡한 비즈니스 로직 구현**: 실제 이커머스의 복잡한 도메인 모델링
3. **현대적 아키텍처 적용**: MSA, Event-Driven, CQRS 패턴 적용
4. **운영 노하우 습득**: 모니터링, 장애 대응, 성능 최적화

---

## 🎯 핵심 목표

### 1. 기능적 목표

#### 1.1 고객 경험 (Customer Experience)
- ✅ **빠른 상품 검색**: 수천만 개 상품 중 1초 이내 검색
- ✅ **개인화 추천**: AI 기반 상품 추천 시스템
- ✅ **원클릭 구매**: 최소 클릭으로 구매 완료
- ✅ **실시간 배송 추적**: 현재 배송 위치 실시간 확인
- ✅ **로켓배송**: 당일/새벽 배송 시스템

#### 1.2 판매자 경험 (Seller Experience)
- ✅ **간편한 상품 등록**: 엑셀 일괄 업로드, 템플릿 제공
- ✅ **재고 관리**: 실시간 재고 동기화, 알림
- ✅ **정산 시스템**: 자동 정산, 세금계산서 발행
- ✅ **판매 분석**: 대시보드, 매출 통계

#### 1.3 운영 효율성 (Operational Excellence)
- ✅ **주문 처리 자동화**: 주문 → 결제 → 배송 자동화
- ✅ **CS 시스템**: 문의, 반품, 환불 처리
- ✅ **배송 최적화**: 물류센터 기반 최적 배송 경로
- ✅ **재고 최적화**: 예측 기반 재고 관리

### 2. 비기능적 목표

#### 2.1 성능 (Performance)
- **응답 시간**: API 평균 응답 시간 < 100ms
- **처리량**: 초당 10,000+ TPS 처리
- **검색 속도**: 상품 검색 < 1초
- **페이지 로딩**: 초기 페이지 로딩 < 2초

#### 2.2 확장성 (Scalability)
- **수평 확장**: Auto Scaling으로 무한 확장 가능
- **데이터베이스**: 샤딩으로 데이터 분산
- **캐싱**: Multi-level 캐싱 전략
- **CDN**: 정적 자원 글로벌 배포

#### 2.3 가용성 (Availability)
- **서비스 가동률**: 99.99% (연간 52분 이내 다운타임)
- **무중단 배포**: Blue-Green, Canary 배포
- **장애 복구**: Circuit Breaker, Fallback 패턴
- **백업**: 실시간 백업 및 재해 복구

#### 2.4 보안 (Security)
- **인증/인가**: OAuth 2.0, JWT 기반 보안
- **데이터 암호화**: TLS 1.3, AES-256 암호화
- **개인정보 보호**: GDPR, 개인정보보호법 준수
- **결제 보안**: PCI-DSS 준수

---

## 💼 비즈니스 요구사항

### 1. 핵심 비즈니스 모델

#### 1.1 수수료 구조
```
판매 수수료 = 판매가 × 카테고리별 수수료율(5~15%)
                + 결제 수수료(2.5%)
                + 배송비 지원금(선택)
```

#### 1.2 로켓배송 정책
| 배송 유형 | 주문 마감 | 배송 완료 | 배송비 |
|----------|---------|----------|-------|
| 로켓배송 | 자정 | 익일 오전 | 무료 (19,800원 이상) |
| 로켓프레시 | 오전 7시 | 당일 오전 7시 전 | 무료 (15,000원 이상) |
| 새벽배송 | 오후 11시 | 익일 오전 7시 전 | 무료 (30,000원 이상) |
| 일반배송 | - | 2-3일 | 3,000원 (30,000원 이상 무료) |

#### 1.3 회원 등급 시스템
| 등급 | 조건 | 혜택 |
|-----|------|------|
| **Bronze** | 신규 가입 | 1% 적립 |
| **Silver** | 최근 6개월 10만원 이상 | 2% 적립, 무료배송 쿠폰 |
| **Gold** | 최근 6개월 50만원 이상 | 3% 적립, 추가 할인 |
| **VIP** | 최근 6개월 100만원 이상 | 5% 적립, 전용 CS |
| **로켓와우** | 월 2,900원 구독 | 무제한 무료배송, 특가 |

### 2. 핵심 비즈니스 프로세스

#### 2.1 주문 플로우
```
고객 주문 → 재고 예약 → 결제 처리 → 주문 확정
    ↓
판매자 확인 → 상품 준비 → 물류센터 입고 → 포장
    ↓
배송 출발 → 배송중 → 배송 완료 → 구매 확정
    ↓
정산 → 판매자 입금 → 리뷰 작성
```

#### 2.2 반품/환불 플로우
```
반품 신청 → 사유 확인 → 승인/거부
    ↓
수거 요청 → 물류센터 도착 → 검수
    ↓
환불 승인 → 결제 취소 → 재고 복구 → 완료
```

#### 2.3 쿠폰 시스템
- **발급 방식**: 자동 발급, 선착순, 조건부
- **할인 유형**: 정액, 정률, 배송비 무료
- **중복 적용**: 최대 3개까지 중복 가능 (단, 동일 카테고리 제외)
- **유효기간**: 발급일로부터 7~90일

---

## 🏗️ 시스템 아키텍처

### 1. 전체 시스템 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend Layer                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                 │
│  │   Web      │  │   Mobile   │  │   Admin    │                 │
│  │  (React)   │  │(React Native)│  │   Panel    │                │
│  └────────────┘  └────────────┘  └────────────┘                 │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS/HTTP2
┌────────────────────────────┼────────────────────────────────────┐
│                    API Gateway Layer                             │
│  ┌──────────────────────────────────────────────────┐            │
│  │  Spring Cloud Gateway / Kong / AWS API Gateway   │            │
│  │  - Rate Limiting                                 │            │
│  │  - Authentication                                │            │
│  │  - Load Balancing                                │            │
│  │  - Circuit Breaker                               │            │
│  └──────────────────────────────────────────────────┘            │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                    Service Mesh (Istio)                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Member     │    │   Product    │    │    Order     │
│   Service    │    │   Service    │    │   Service    │
│ (Spring Boot)│    │ (Spring Boot)│    │ (Spring Boot)│
└──────────────┘    └──────────────┘    └──────────────┘
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Payment    │    │  Inventory   │    │   Delivery   │
│   Service    │    │   Service    │    │   Service    │
└──────────────┘    └──────────────┘    └──────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Search      │    │   Coupon     │    │   Seller     │
│  Service     │    │   Service    │    │   Service    │
│(Elasticsearch)│   │              │    │              │
└──────────────┘    └──────────────┘    └──────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                    Message Queue Layer                           │
│  ┌──────────────────────────────────────────────────┐            │
│  │  Apache Kafka / RabbitMQ / AWS SQS               │            │
│  │  - Order Events                                  │            │
│  │  - Inventory Events                              │            │
│  │  - Notification Events                           │            │
│  └──────────────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                      Data Layer                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  MySQL   │  │  Redis   │  │  MongoDB │  │   S3     │        │
│  │ (Master  │  │ (Cache)  │  │  (Logs)  │  │ (Images) │        │
│  │ + Slave) │  │          │  │          │  │          │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└─────────────────────────────────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                   Observability Layer                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │Prometheus│  │  Grafana │  │   ELK    │  │  Jaeger  │        │
│  │(Metrics) │  │  (Viz)   │  │  (Logs)  │  │ (Trace)  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

### 2. 마이크로서비스 분리 전략

#### 2.1 도메인별 서비스 분리

| 서비스 | 책임 | 주요 기능 | DB |
|--------|------|----------|-----|
| **Member Service** | 회원 관리 | 회원가입, 로그인, 프로필, 배송지 | MySQL |
| **Product Service** | 상품 관리 | 상품 등록, 카테고리, 옵션 | MySQL |
| **Inventory Service** | 재고 관리 | 재고 추가/차감, 동시성 제어 | MySQL + Redis |
| **Order Service** | 주문 처리 | 주문 생성, 상태 관리, 취소 | MySQL |
| **Payment Service** | 결제 처리 | 결제, 환불, PG 연동 | MySQL |
| **Delivery Service** | 배송 관리 | 배송 추적, 물류센터 관리 | MySQL |
| **Search Service** | 검색 | 상품 검색, 자동완성, 필터링 | Elasticsearch |
| **Coupon Service** | 쿠폰 관리 | 쿠폰 발급, 사용, 검증 | MySQL + Redis |
| **Review Service** | 리뷰 관리 | 리뷰 작성, 평점, 베스트 리뷰 | MongoDB |
| **Seller Service** | 판매자 관리 | 판매자 등록, 정산, 통계 | MySQL |
| **Notification Service** | 알림 | 이메일, SMS, 푸시 알림 | MongoDB |
| **Recommendation Service** | 추천 | AI 기반 상품 추천 | Redis + Python |

#### 2.2 서비스 간 통신 패턴

**동기 통신 (Synchronous)**
```java
// RestTemplate / WebClient / Feign
@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable Long id);
}
```

**비동기 통신 (Asynchronous)**
```java
// Kafka Event Publishing
@Service
public class OrderEventPublisher {
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        kafkaTemplate.send("order.created", event);
    }
}

// Kafka Event Consuming
@KafkaListener(topics = "order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 재고 차감
    inventoryService.decrease(event.getOrderItems());
}
```

### 3. 데이터 관리 전략

#### 3.1 데이터베이스 패턴

**Database per Service**
- 각 마이크로서비스는 독립적인 데이터베이스 소유
- 서비스 간 데이터베이스 직접 접근 금지
- API 또는 이벤트를 통한 데이터 공유

**CQRS (Command Query Responsibility Segregation)**
```
Write Model (Command)          Read Model (Query)
      ↓                              ↓
  MySQL (Master)              Redis Cache + MySQL (Slave)
      ↓                              ↓
  Event Publishing            Event Consuming
      ↓                              ↓
  Kafka → → → → → → → → → → → Read DB Update
```

#### 3.2 캐싱 전략

**Multi-Level Caching**
```
L1: Browser Cache (Static Assets)
    ↓
L2: CDN (CloudFlare / AWS CloudFront)
    ↓
L3: Redis Cache (Hot Data)
    ↓
L4: Application Cache (Caffeine)
    ↓
L5: Database Query Cache
```

**Cache-Aside Pattern**
```java
@Service
public class ProductService {
    public Product getProduct(Long id) {
        // 1. 캐시 확인
        Product cached = redisTemplate.get("product:" + id);
        if (cached != null) return cached;

        // 2. DB 조회
        Product product = productRepository.findById(id);

        // 3. 캐시 저장
        redisTemplate.set("product:" + id, product, 3600);

        return product;
    }
}
```

---

## 🛠️ 기술 스택

### 1. Backend

#### 1.1 Core Framework
| 기술 | 버전 | 용도 |
|-----|------|------|
| Java | 17 LTS | 주 언어 |
| Spring Boot | 3.2.x | 웹 프레임워크 |
| Spring WebFlux | 3.2.x | 리액티브 프로그래밍 |
| Spring Cloud | 2023.x | 마이크로서비스 |
| Spring Security | 6.x | 보안 |
| Spring Data JPA | 3.2.x | ORM |
| Spring Data R2DBC | 3.2.x | 리액티브 DB |
| QueryDSL | 5.0.x | 타입 세이프 쿼리 |

#### 1.2 Message Queue
| 기술 | 용도 |
|-----|------|
| Apache Kafka | 이벤트 스트리밍 |
| RabbitMQ | 작업 큐 |
| Redis Pub/Sub | 실시간 알림 |

#### 1.3 Database
| 기술 | 용도 |
|-----|------|
| MySQL 8.0 | 메인 DB |
| Redis 7.x | 캐시, 세션 |
| MongoDB | 로그, 리뷰 |
| Elasticsearch | 검색 |

### 2. Frontend

| 기술 | 용도 |
|-----|------|
| React 18 | 웹 UI |
| Next.js 14 | SSR, SEO |
| TypeScript | 타입 안정성 |
| Tailwind CSS | 스타일링 |
| React Query | 서버 상태 관리 |
| Zustand | 클라이언트 상태 관리 |
| React Native | 모바일 앱 |

### 3. DevOps & Infrastructure

| 기술 | 용도 |
|-----|------|
| Docker | 컨테이너화 |
| Kubernetes | 오케스트레이션 |
| Helm | K8s 패키징 |
| ArgoCD | GitOps |
| Jenkins | CI/CD |
| GitHub Actions | CI/CD |
| Terraform | IaC |
| Prometheus | 메트릭 수집 |
| Grafana | 모니터링 대시보드 |
| ELK Stack | 로그 수집/분석 |
| Jaeger | 분산 트레이싱 |

### 4. Cloud Provider

**AWS 기반 구성**
- **Compute**: EKS (Kubernetes), EC2, Lambda
- **Storage**: S3, EBS, EFS
- **Database**: RDS (MySQL), ElastiCache (Redis), DocumentDB
- **Network**: VPC, ALB, CloudFront, Route53
- **Message**: SQS, SNS, MSK (Kafka)
- **Monitoring**: CloudWatch, X-Ray

---

## 📊 개발 전략

### 1. 개발 방법론

#### 1.1 애자일 스크럼
- **스프린트**: 2주 단위
- **데일리 스탠드업**: 매일 오전 10시
- **스프린트 리뷰**: 격주 금요일
- **회고**: 스프린트 종료 후

#### 1.2 코드 품질 관리
```yaml
Code Review:
  - Pull Request 필수
  - 2명 이상 승인 후 머지
  - 자동화된 테스트 통과 필수

Testing:
  - Unit Test Coverage > 80%
  - Integration Test 필수
  - E2E Test (주요 시나리오)

Static Analysis:
  - SonarQube
  - CheckStyle
  - SpotBugs
```

### 2. 브랜치 전략 (Git Flow)

```
main (production)
  ↑
  ← release/v1.0.0
      ↑
      ← develop
          ↑
          ← feature/member-service
          ← feature/product-service
          ← hotfix/critical-bug
```

### 3. 배포 전략

#### 3.1 배포 파이프라인
```
Code Commit → Build → Test → Image Build → Push to Registry
    ↓
Security Scan → Deploy to Dev → Integration Test
    ↓
Deploy to Staging → E2E Test → Performance Test
    ↓
Deploy to Production (Canary) → Monitor → Full Rollout
```

#### 3.2 배포 방식
- **Blue-Green Deployment**: 무중단 배포
- **Canary Deployment**: 점진적 배포 (10% → 50% → 100%)
- **Rolling Update**: Kubernetes 기본 배포

---

## 📅 프로젝트 타임라인

### 전체 일정 (24주)

```
Phase 1: 기반 구축 (Week 1-8)
Phase 2: 핵심 기능 (Week 9-16)
Phase 3: 고급 기능 (Week 17-24)
```

### Phase 1: 기반 구축 (8주)

| 주차 | 작업 내용 |
|-----|---------|
| **Week 1-2** | 프로젝트 셋업, CI/CD, 인프라 구성 |
| **Week 3-4** | Member Service, Auth 구현 |
| **Week 5-6** | Product Service, Category 구현 |
| **Week 7-8** | Inventory Service, 동시성 제어 |

### Phase 2: 핵심 기능 (8주)

| 주차 | 작업 내용 |
|-----|---------|
| **Week 9-10** | Order Service, 주문 상태 머신 |
| **Week 11-12** | Payment Service, PG 연동 |
| **Week 13-14** | Delivery Service, 배송 추적 |
| **Week 15-16** | Search Service, Elasticsearch 연동 |

### Phase 3: 고급 기능 (8주)

| 주차 | 작업 내용 |
|-----|---------|
| **Week 17-18** | Coupon Service, Review Service |
| **Week 19-20** | Seller Service, 정산 시스템 |
| **Week 21-22** | Recommendation Service, AI 추천 |
| **Week 23-24** | 성능 최적화, 보안 강화, 런칭 준비 |

---

## 📈 성공 지표 (KPI)

### 1. 기술적 지표

| 지표 | 목표 | 측정 방법 |
|-----|------|----------|
| API 응답 시간 | < 100ms | Prometheus |
| TPS | > 10,000 | Load Testing |
| 서비스 가동률 | 99.99% | CloudWatch |
| 에러율 | < 0.1% | Sentry |
| 코드 커버리지 | > 80% | JaCoCo |

### 2. 비즈니스 지표

| 지표 | 목표 | 측정 방법 |
|-----|------|----------|
| MAU (월간 활성 사용자) | 100만+ | Google Analytics |
| 전환율 | > 3% | GA + 자체 분석 |
| 평균 주문 금액 | 50,000원+ | 주문 데이터 |
| 재구매율 | > 30% | 고객 데이터 |
| 고객 만족도 | > 4.5/5 | 설문 조사 |

---

## 🚧 리스크 관리

### 주요 리스크 및 대응 방안

| 리스크 | 영향도 | 대응 방안 |
|--------|--------|----------|
| 대규모 트래픽 폭증 | 높음 | Auto Scaling, CDN, 캐싱 |
| 데이터베이스 병목 | 높음 | Read Replica, Sharding, CQRS |
| 결제 장애 | 치명적 | 다중 PG사 연동, Fallback |
| 보안 취약점 | 치명적 | 정기 보안 감사, Penetration Test |
| 서비스 장애 | 높음 | Circuit Breaker, Graceful Degradation |

---

**다음 문서**: [plan-2-domain-design.md](plan-2-domain-design.md) - 도메인 모델 상세 설계
