# JMeter Auth API Load Test

## 사전 준비

### 1. 서비스 실행 확인
```bash
# Docker 컨테이너 상태 확인 (MySQL, Redis)
docker-compose ps

# 애플리케이션 실행 확인
curl http://localhost:8080/actuator/health

# 또는 직접 애플리케이션 실행
./gradlew bootRun
```

### 2. 테스트 사용자 생성 (Login 테스트용)
```bash
# Signup API로 테스트 계정 생성
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!@",
    "name": "테스트유저",
    "phone": "010-1234-5678"
  }'
```

## 테스트 시나리오

### Scenario 1: Signup Load Test
- **동시 사용자**: 100명
- **Ramp-up**: 10초 (초당 10명씩 증가)
- **반복 횟수**: 10회
- **총 요청 수**: 1,000회 (100 threads × 10 loops)
- **예상 소요 시간**: ~10-30초

### Scenario 2: Login Load Test
- **동시 사용자**: 200명
- **Ramp-up**: 20초 (초당 10명씩 증가)
- **반복 횟수**: 20회
- **총 요청 수**: 4,000회 (200 threads × 20 loops)
- **예상 소요 시간**: ~20-60초

## 실행 방법

### 방법 1: GUI 모드 (첫 실행 시 권장)
```bash
# Windows
jmeter.bat -t jmeter/auth-load-test.jmx

# Linux/Mac
jmeter -t jmeter/auth-load-test.jmx
```

GUI에서 확인할 수 있는 것:
- View Results Tree: 요청/응답 상세 내용
- Summary Report: 통계 요약
- Graph Results: 성능 그래프

**실행 방법**:
1. JMeter GUI가 열리면 상단의 녹색 "Start" 버튼 클릭
2. 테스트 실행 중 리스너에서 실시간 결과 확인
3. 완료 후 결과 분석

### 방법 2: CLI 모드 (실제 부하 테스트 권장)
```bash
# 기본 실행 (결과를 CSV로 저장)
jmeter -n -t jmeter/auth-load-test.jmx \
  -l jmeter/results/auth-load-test-results.jtl

# HTML 리포트 자동 생성 (권장)
jmeter -n -t jmeter/auth-load-test.jmx \
  -l jmeter/results/auth-load-test-results.jtl \
  -e -o jmeter/reports/auth-load-test-report

# Windows에서 실행
jmeter.bat -n -t jmeter\auth-load-test.jmx -l jmeter\results\auth-load-test-results.jtl -e -o jmeter\reports\auth-load-test-report
```

**CLI 모드 장점**:
- GUI 오버헤드 없음 (더 정확한 성능 측정)
- 자동화 가능
- HTML 리포트 생성

## 결과 해석

### 1. Summary Report (요약 통계)
```
Label              | Samples | Avg(ms) | Min | Max  | Std Dev | Error% | Throughput | KB/sec
POST /auth/signup  | 1000    | 250     | 50  | 1000 | 120     | 0%     | 100/sec    | 25
POST /auth/login   | 4000    | 180     | 40  | 800  | 90      | 0%     | 200/sec    | 50
```

**주요 지표**:
- **Samples**: 총 요청 수
- **Avg (Average)**: 평균 응답 시간 (ms)
  - ✅ 좋음: < 200ms
  - ⚠️ 보통: 200-500ms
  - ❌ 나쁨: > 500ms
- **Min/Max**: 최소/최대 응답 시간
- **Std Dev**: 표준편차 (일관성 지표)
  - ✅ 낮을수록 안정적
- **Error %**: 에러율
  - ✅ 목표: 0%
  - ⚠️ 허용: < 1%
  - ❌ 문제: > 5%
- **Throughput**: 처리량 (초당 요청 수)
  - ✅ WebFlux 목표: > 1,000 req/sec

### 2. HTML Report (상세 분석)
HTML 리포트 생성 후 브라우저로 열기:
```bash
# Windows
start jmeter/reports/auth-load-test-report/index.html

# Linux/Mac
open jmeter/reports/auth-load-test-report/index.html
```

**주요 차트**:
- **Over Time**: 시간별 응답 시간 변화
- **Throughput**: 처리량 추이
- **Response Times Percentiles**: 95%, 99% 응답 시간
- **Active Threads Over Time**: 동시 사용자 수 변화

### 3. WebFlux 성능 검증 포인트

**기대 성능** (Netty Event Loop 기반):
```
목표 지표:
- Throughput: > 1,000 req/sec (Tomcat 대비 5-10배)
- Average Response Time: < 200ms
- 95th Percentile: < 500ms
- Error Rate: 0%
- CPU Usage: < 50% (Event Loop 효율)
```

**성능 비교**:
```
Spring MVC (Tomcat):
- 처리량: ~200 req/sec
- 스레드: 200개 (Thread Pool 한계)
- 메모리: 높음 (스레드당 1MB)

Spring WebFlux (Netty):
- 처리량: ~1,000+ req/sec
- 스레드: 8개 (Event Loop)
- 메모리: 낮음 (스레드 재사용)
```

## 성능 모니터링

테스트 중 시스템 모니터링:
```bash
# JVM 메모리 사용량 확인
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 스레드 수 확인
curl http://localhost:8080/actuator/metrics/jvm.threads.live

# CPU 사용량 확인
curl http://localhost:8080/actuator/metrics/process.cpu.usage
```

## 문제 해결

### 에러율이 높을 때 (> 5%)
```bash
# 로그 확인
docker-compose logs -f app

# 데이터베이스 연결 확인
docker-compose ps mysql

# Redis 연결 확인
docker-compose ps redis
```

**일반적인 원인**:
1. 데이터베이스 커넥션 부족
2. Redis 연결 타임아웃
3. BCrypt 암호화 과부하 (Schedulers.boundedElastic() 스레드 부족)

### 응답 시간이 느릴 때 (> 500ms)
1. **데이터베이스 쿼리 최적화**: 인덱스 확인
2. **커넥션 풀 튜닝**: R2DBC pool size 증가
3. **Redis 캐싱**: 중복 조회 최소화
4. **프로파일링**: 병목 구간 식별

## 추가 테스트 시나리오

### 1. Spike Test (급증 부하)
```xml
<!-- Ramp-up을 1초로 변경 -->
<stringProp name="ThreadGroup.ramp_time">1</stringProp>
```

### 2. Endurance Test (지구력 테스트)
```xml
<!-- Duration을 1시간(3600초)으로 설정 -->
<boolProp name="ThreadGroup.scheduler">true</boolProp>
<stringProp name="ThreadGroup.duration">3600</stringProp>
```

### 3. Token Refresh Test
새로운 Thread Group 추가:
```bash
POST /api/v1/auth/refresh
Body: { "refreshToken": "..." }
```

## 결과 파일

- **JTL 파일**: `jmeter/results/auth-load-test-results.jtl` (CSV 형식)
- **HTML 리포트**: `jmeter/reports/auth-load-test-report/index.html`
- **CSV Export**: `jmeter/results/auth-load-test-results.csv` (Summary Report에서 자동 생성)

## 참고 사항

1. **GUI vs CLI**:
   - 테스트 설계/디버깅: GUI 모드
   - 실제 부하 테스트: CLI 모드 (GUI 오버헤드 제거)

2. **Ramp-up 중요성**:
   - 0초: 모든 스레드 동시 시작 (서버 과부하 가능)
   - 적절한 값: 점진적 부하 증가 (현실적)

3. **테스트 환경**:
   - 로컬: 개발/디버깅용
   - 운영 유사 환경: 실제 성능 측정용

4. **결과 신뢰성**:
   - 테스트 머신 사양도 성능에 영향
   - 3회 이상 실행 후 평균값 사용 권장
