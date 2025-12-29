# 쿠팡 클론 프로젝트 기획서 (6/6) - 배포 및 운영

> **Kubernetes 기반 배포, 모니터링, 장애 대응**

---

## 📋 목차
1. [Kubernetes 배포](#kubernetes-배포)
2. [모니터링 시스템](#모니터링-시스템)
3. [로깅 시스템](#로깅-시스템)
4. [성능 튜닝](#성능-튜닝)
5. [장애 대응](#장애-대응)
6. [운영 가이드](#운영-가이드)

---

## ☸️ Kubernetes 배포

### 1. Namespace 구성

**namespace.yaml**:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: coupang-production
  labels:
    environment: production
---
apiVersion: v1
kind: Namespace
metadata:
  name: coupang-staging
  labels:
    environment: staging
---
apiVersion: v1
kind: Namespace
metadata:
  name: coupang-development
  labels:
    environment: development
```

### 2. ConfigMap & Secret

**configmap.yaml**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: coupang-production
data:
  # Application 설정
  SPRING_PROFILES_ACTIVE: "production"
  SERVER_PORT: "8080"

  # Database 설정
  DB_HOST: "mysql-service.coupang-production.svc.cluster.local"
  DB_PORT: "3306"
  DB_NAME: "coupang"

  # Redis 설정
  REDIS_HOST: "redis-service.coupang-production.svc.cluster.local"
  REDIS_PORT: "6379"

  # Kafka 설정
  KAFKA_BOOTSTRAP_SERVERS: "kafka-service.coupang-production.svc.cluster.local:9092"

  # Elasticsearch 설정
  ES_HOST: "elasticsearch-service.coupang-production.svc.cluster.local"
  ES_PORT: "9200"

  # Logging 설정
  LOG_LEVEL: "INFO"
  LOG_PATH: "/var/log/application"
```

**secret.yaml**:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: coupang-production
type: Opaque
data:
  # Base64 encoded values
  DB_USERNAME: bXlzcWw=  # mysql
  DB_PASSWORD: cGFzc3dvcmQ=  # password
  REDIS_PASSWORD: cmVkaXNwYXNz  # redispass
  JWT_SECRET: c2VjcmV0a2V5MTIzNDU2Nzg5MA==
  TOSS_SECRET_KEY: dG9zc19zZWNyZXRfa2V5
```

### 3. Member Service Deployment

**member-service-deployment.yaml**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: member-service
  namespace: coupang-production
  labels:
    app: member-service
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: member-service
  template:
    metadata:
      labels:
        app: member-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
      - name: member-service
        image: ghcr.io/coupang/member-service:v1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 9090
          name: grpc

        # Environment Variables
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secret

        # Resource Limits
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"

        # Health Checks
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3

        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3

        # Lifecycle Hooks
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]

        # Volume Mounts
        volumeMounts:
        - name: logs
          mountPath: /var/log/application
        - name: config
          mountPath: /app/config
          readOnly: true

      # Volumes
      volumes:
      - name: logs
        emptyDir: {}
      - name: config
        configMap:
          name: app-config

      # Image Pull Secrets
      imagePullSecrets:
      - name: ghcr-secret

      # Affinity (다른 노드에 분산)
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - member-service
              topologyKey: kubernetes.io/hostname
---
apiVersion: v1
kind: Service
metadata:
  name: member-service
  namespace: coupang-production
  labels:
    app: member-service
spec:
  type: ClusterIP
  selector:
    app: member-service
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: grpc
    port: 9090
    targetPort: 9090
```

### 4. Horizontal Pod Autoscaler (HPA)

**hpa.yaml**:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: member-service-hpa
  namespace: coupang-production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: member-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 4
        periodSeconds: 30
      selectPolicy: Max
```

### 5. Ingress 설정

**ingress.yaml**:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: coupang-ingress
  namespace: coupang-production
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "1000"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://coupang.example.com"
spec:
  tls:
  - hosts:
    - api.coupang.example.com
    secretName: coupang-tls
  rules:
  - host: api.coupang.example.com
    http:
      paths:
      - path: /api/v1/members
        pathType: Prefix
        backend:
          service:
            name: member-service
            port:
              number: 8080
      - path: /api/v1/products
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 8080
      - path: /api/v1/orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 8080
      - path: /api/v1/payments
        pathType: Prefix
        backend:
          service:
            name: payment-service
            port:
              number: 8080
```

### 6. Database StatefulSet (MySQL)

**mysql-statefulset.yaml**:
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
  namespace: coupang-production
spec:
  serviceName: mysql
  replicas: 3
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        ports:
        - containerPort: 3306
          name: mysql
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secret
              key: DB_PASSWORD
        - name: MYSQL_DATABASE
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: DB_NAME
        volumeMounts:
        - name: mysql-data
          mountPath: /var/lib/mysql
        - name: mysql-config
          mountPath: /etc/mysql/conf.d
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
      volumes:
      - name: mysql-config
        configMap:
          name: mysql-config
  volumeClaimTemplates:
  - metadata:
      name: mysql-data
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 100Gi
---
apiVersion: v1
kind: Service
metadata:
  name: mysql
  namespace: coupang-production
spec:
  clusterIP: None
  selector:
    app: mysql
  ports:
  - port: 3306
    name: mysql
```

---

## 📊 모니터링 시스템

### 1. Prometheus 설정

**prometheus-config.yaml**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    scrape_configs:
      # Kubernetes API Server
      - job_name: 'kubernetes-apiservers'
        kubernetes_sd_configs:
        - role: endpoints
        scheme: https
        tls_config:
          ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token

      # Kubernetes Nodes
      - job_name: 'kubernetes-nodes'
        kubernetes_sd_configs:
        - role: node
        relabel_configs:
        - action: labelmap
          regex: __meta_kubernetes_node_label_(.+)

      # Kubernetes Pods
      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
        - role: pod
        relabel_configs:
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
          action: replace
          regex: ([^:]+)(?::\d+)?;(\d+)
          replacement: $1:$2
          target_label: __address__

      # Member Service
      - job_name: 'member-service'
        metrics_path: '/actuator/prometheus'
        kubernetes_sd_configs:
        - role: pod
          namespaces:
            names:
            - coupang-production
        relabel_configs:
        - source_labels: [__meta_kubernetes_pod_label_app]
          action: keep
          regex: member-service
```

### 2. Grafana Dashboard

**member-service-dashboard.json**:
```json
{
  "dashboard": {
    "title": "Member Service Dashboard",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{job=\"member-service\"}[5m])"
          }
        ]
      },
      {
        "title": "Response Time (P95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job=\"member-service\"}[5m]))"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{job=\"member-service\",status=~\"5..\"}[5m])"
          }
        ]
      },
      {
        "title": "JVM Memory Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{job=\"member-service\"} / jvm_memory_max_bytes{job=\"member-service\"} * 100"
          }
        ]
      },
      {
        "title": "Database Connection Pool",
        "targets": [
          {
            "expr": "hikaricp_connections_active{job=\"member-service\"}"
          }
        ]
      }
    ]
  }
}
```

### 3. Alert Rules

**alert-rules.yaml**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-alerts
  namespace: monitoring
data:
  alerts.yml: |
    groups:
    - name: application_alerts
      interval: 30s
      rules:

      # High Error Rate
      - alert: HighErrorRate
        expr: |
          (
            rate(http_server_requests_seconds_count{status=~"5.."}[5m])
            /
            rate(http_server_requests_seconds_count[5m])
          ) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "{{ $labels.job }} has error rate above 5% (current: {{ $value }}%)"

      # High Response Time
      - alert: HighResponseTime
        expr: |
          histogram_quantile(0.95,
            rate(http_server_requests_seconds_bucket[5m])
          ) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time"
          description: "{{ $labels.job }} P95 response time is above 1s (current: {{ $value }}s)"

      # Pod Down
      - alert: PodDown
        expr: up{job=~".*-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Pod is down"
          description: "{{ $labels.job }} pod is down"

      # High CPU Usage
      - alert: HighCPUUsage
        expr: |
          (
            rate(process_cpu_seconds_total[5m]) * 100
          ) > 80
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "{{ $labels.job }} CPU usage is above 80% (current: {{ $value }}%)"

      # High Memory Usage
      - alert: HighMemoryUsage
        expr: |
          (
            jvm_memory_used_bytes / jvm_memory_max_bytes * 100
          ) > 85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "{{ $labels.job }} memory usage is above 85% (current: {{ $value }}%)"

      # Database Connection Pool Exhaustion
      - alert: DatabaseConnectionPoolExhaustion
        expr: |
          (
            hikaricp_connections_active / hikaricp_connections_max * 100
          ) > 90
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool near exhaustion"
          description: "{{ $labels.job }} connection pool usage is above 90%"
```

---

## 📝 로깅 시스템

### 1. ELK Stack 구성

**filebeat-config.yaml**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: filebeat-config
  namespace: logging
data:
  filebeat.yml: |
    filebeat.inputs:
    - type: container
      paths:
        - /var/log/containers/*.log
      processors:
        - add_kubernetes_metadata:
            host: ${NODE_NAME}
            matchers:
            - logs_path:
                logs_path: "/var/log/containers/"

    output.elasticsearch:
      hosts: ['elasticsearch:9200']
      indices:
        - index: "coupang-application-%{+yyyy.MM.dd}"
          when.contains:
            kubernetes.namespace: "coupang-production"

    setup.kibana:
      host: "kibana:5601"

    setup.ilm:
      enabled: true
      policy_name: "coupang-logs"
      rollover_alias: "coupang-logs"
```

### 2. Logback 설정 (Application)

**logback-spring.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <springProperty scope="context" name="applicationName" source="spring.application.name"/>

    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"${applicationName}"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>

    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/application/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/application/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <!-- Async Appender -->
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <appender-ref ref="FILE"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC"/>
    </root>

    <logger name="com.coupang" level="DEBUG"/>
    <logger name="org.springframework.web" level="INFO"/>
    <logger name="org.hibernate.SQL" level="DEBUG"/>
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE"/>
</configuration>
```

---

## ⚡ 성능 튜닝

### 1. JVM 튜닝

**jvm-options.txt**:
```bash
# Heap Size
-Xms1g
-Xmx2g

# GC 설정 (G1GC)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:InitiatingHeapOccupancyPercent=45
-XX:+ParallelRefProcEnabled

# GC 로깅
-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=5,filesize=10M

# Out of Memory
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/heap-dump.hprof

# Performance
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat

# Monitoring
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
```

### 2. Database 튜닝

**mysql-config.cnf**:
```ini
[mysqld]
# InnoDB 설정
innodb_buffer_pool_size = 8G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT

# Connection Pool
max_connections = 500
thread_cache_size = 100

# Query Cache (MySQL 8.0에서는 제거됨)
# query_cache_size = 0

# Slow Query Log
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-query.log
long_query_time = 1

# Binary Log
log_bin = /var/log/mysql/mysql-bin.log
expire_logs_days = 7
max_binlog_size = 100M

# Character Set
character_set_server = utf8mb4
collation_server = utf8mb4_unicode_ci
```

### 3. Redis 튜닝

**redis.conf**:
```conf
# Memory
maxmemory 4gb
maxmemory-policy allkeys-lru

# Persistence
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec

# Network
tcp-backlog 511
timeout 0
tcp-keepalive 300

# Performance
hz 10
```

---

## 🚨 장애 대응

### 1. Circuit Breaker 설정

**CircuitBreakerConfig.java**:
```java
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                // 실패율 50% 이상 시 Circuit Open
                .failureRateThreshold(50)
                // 최소 10번의 호출 후 실패율 계산
                .minimumNumberOfCalls(10)
                // Half-Open 상태에서 5번 호출 테스트
                .permittedNumberOfCallsInHalfOpenState(5)
                // 5초 대기 후 Half-Open 상태로 전환
                .waitDurationInOpenState(Duration.ofSeconds(5))
                // Sliding Window 크기: 100
                .slidingWindowSize(100)
                .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                // 3초 타임아웃
                .timeoutDuration(Duration.ofSeconds(3))
                .build())
            .build());
    }
}
```

### 2. Rate Limiting

**RateLimitingFilter.java**:
```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    // 사용자당 분당 100회 제한
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = extractUserId(request);
        String key = "rate_limit:" + userId;

        Long requestCount = redisTemplate.opsForValue().increment(key);

        if (requestCount == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }

        if (requestCount > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

### 3. Graceful Shutdown

**application.yml**:
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

---

## 📖 운영 가이드

### 1. 배포 절차

```bash
# 1. 이미지 빌드
docker build -t ghcr.io/coupang/member-service:v1.0.1 .

# 2. 이미지 푸시
docker push ghcr.io/coupang/member-service:v1.0.1

# 3. Kubernetes 배포 (Canary)
kubectl set image deployment/member-service \
  member-service=ghcr.io/coupang/member-service:v1.0.1 \
  -n coupang-production

# 4. 배포 상태 확인
kubectl rollout status deployment/member-service -n coupang-production

# 5. 문제 발생 시 롤백
kubectl rollout undo deployment/member-service -n coupang-production
```

### 2. 모니터링 체크리스트

**일일 체크**:
- [ ] 서비스 가동률 확인
- [ ] 에러율 확인 (< 0.1%)
- [ ] 응답 시간 확인 (P95 < 100ms)
- [ ] CPU/메모리 사용률
- [ ] 데이터베이스 커넥션 풀 상태

**주간 체크**:
- [ ] 로그 분석 (에러 패턴)
- [ ] 성능 트렌드 분석
- [ ] 디스크 사용량
- [ ] 백업 상태 확인

**월간 체크**:
- [ ] 보안 패치 적용
- [ ] 의존성 업데이트
- [ ] 성능 테스트
- [ ] 재해 복구 테스트

### 3. 장애 대응 시나리오

**시나리오 1: 특정 서비스 장애**
```bash
# 1. 로그 확인
kubectl logs -f deployment/member-service -n coupang-production

# 2. Pod 재시작
kubectl rollout restart deployment/member-service -n coupang-production

# 3. 이전 버전으로 롤백
kubectl rollout undo deployment/member-service -n coupang-production
```

**시나리오 2: 데이터베이스 장애**
```bash
# 1. 데이터베이스 상태 확인
kubectl exec -it mysql-0 -n coupang-production -- mysql -u root -p

# 2. Replica 확인
SHOW SLAVE STATUS\G

# 3. Failover (수동)
kubectl delete pod mysql-0 -n coupang-production
```

**시나리오 3: 대규모 트래픽 폭증**
```bash
# 1. HPA 수동 스케일 아웃
kubectl scale deployment member-service --replicas=20 -n coupang-production

# 2. Rate Limiting 강화
kubectl patch configmap nginx-config -n ingress-nginx --patch '{"data":{"rate-limit":"500"}}'

# 3. 캐시 TTL 연장
# Redis에서 캐시 만료 시간 조정
```

### 4. 백업 및 복구

**데이터베이스 백업**:
```bash
# 전체 백업 (매일 새벽 2시)
mysqldump -u root -p --all-databases --single-transaction \
  --quick --lock-tables=false > backup-$(date +\%Y\%m\%d).sql

# S3 업로드
aws s3 cp backup-$(date +\%Y\%m\%d).sql \
  s3://coupang-backups/mysql/$(date +\%Y\%m\%d)/
```

**데이터 복구**:
```bash
# 백업 다운로드
aws s3 cp s3://coupang-backups/mysql/20240101/backup-20240101.sql .

# 복구
mysql -u root -p < backup-20240101.sql
```

---

## 🎯 프로젝트 완료 체크리스트

### 개발 완료
- [x] Member Service 구현
- [x] Product Service 구현
- [x] Inventory Service 구현
- [x] Order Service 구현
- [x] Payment Service 구현
- [x] Delivery Service 구현
- [x] Search Service 구현
- [x] Coupon Service 구현
- [x] Review Service 구현
- [x] Seller Service 구현
- [x] Recommendation Service 구현

### 테스트 완료
- [ ] 단위 테스트 (Coverage > 80%)
- [ ] 통합 테스트
- [ ] E2E 테스트
- [ ] 성능 테스트 (10,000 TPS)
- [ ] 부하 테스트
- [ ] 보안 테스트

### 배포 완료
- [ ] Kubernetes 배포
- [ ] CI/CD 파이프라인
- [ ] 모니터링 시스템
- [ ] 로깅 시스템
- [ ] 알림 시스템
- [ ] 백업 시스템

### 문서화 완료
- [x] API 문서 (Swagger)
- [x] 아키텍처 문서
- [x] 배포 가이드
- [x] 운영 가이드
- [ ] 트러블슈팅 가이드

---

## 🎉 맺음말

본 기획서는 쿠팡과 같은 대규모 이커머스 플랫폼을 구축하기 위한 상세한 가이드입니다.

### 핵심 학습 포인트
1. **마이크로서비스 아키텍처**: 서비스 분리 및 통신
2. **대규모 트래픽 처리**: 캐싱, 샤딩, 로드 밸런싱
3. **동시성 제어**: 재고 관리, 쿠폰 발급
4. **이벤트 기반 아키텍처**: Kafka를 이용한 비동기 처리
5. **Kubernetes 운영**: 배포, 모니터링, 장애 대응
6. **성능 최적화**: JVM 튜닝, 데이터베이스 최적화

### 다음 단계
- 실제 코드 구현
- 성능 벤치마크
- 보안 강화
- 사용자 피드백 수집
- 지속적인 개선

**Good Luck! 🚀**
