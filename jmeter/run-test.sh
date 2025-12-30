#!/bin/bash

# JMeter Auth API Load Test Runner
# Usage: ./jmeter/run-test.sh

echo "========================================="
echo "JMeter Auth API Load Test"
echo "========================================="

# 1. 사전 체크
echo ""
echo "[1/5] Checking prerequisites..."

# Docker 컨테이너 확인
if ! docker-compose ps | grep -q "Up"; then
    echo "❌ Error: Docker containers are not running"
    echo "Run: docker-compose up -d"
    exit 1
fi
echo "✅ Docker containers are running"

# 애플리케이션 확인
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "❌ Error: Application is not running"
    echo "Run: ./gradlew bootRun"
    exit 1
fi
echo "✅ Application is running"

# 2. 테스트 사용자 생성 (Login 테스트용)
echo ""
echo "[2/5] Creating test user..."
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!@",
    "name": "테스트유저",
    "phone": "010-1234-5678"
  }' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s || echo "ℹ️  User may already exist (OK for login test)"

# 3. 결과 디렉토리 정리
echo ""
echo "[3/5] Preparing result directories..."
rm -rf jmeter/results/* jmeter/reports/*
mkdir -p jmeter/results jmeter/reports
echo "✅ Directories ready"

# 4. JMeter 테스트 실행
echo ""
echo "[4/5] Running JMeter load test..."
echo "Test scenarios:"
echo "  - Signup: 100 threads × 10 loops = 1,000 requests"
echo "  - Login:  200 threads × 20 loops = 4,000 requests"
echo ""

if command -v jmeter &> /dev/null; then
    jmeter -n -t jmeter/auth-load-test.jmx \
      -l jmeter/results/auth-load-test-results.jtl \
      -e -o jmeter/reports/auth-load-test-report

    echo ""
    echo "✅ Test completed!"
else
    echo "❌ Error: JMeter command not found"
    echo "Please run manually:"
    echo "  jmeter -n -t jmeter/auth-load-test.jmx -l jmeter/results/auth-load-test-results.jtl -e -o jmeter/reports/auth-load-test-report"
    exit 1
fi

# 5. 결과 출력
echo ""
echo "[5/5] Test Results"
echo "========================================="
echo ""
echo "📊 HTML Report:"
echo "  file://$(pwd)/jmeter/reports/auth-load-test-report/index.html"
echo ""
echo "📄 Raw Results:"
echo "  $(pwd)/jmeter/results/auth-load-test-results.jtl"
echo ""
echo "🚀 Quick Stats:"
if [ -f "jmeter/reports/auth-load-test-report/statistics.json" ]; then
    # Parse statistics (basic)
    echo "  (Open HTML report for detailed analysis)"
else
    echo "  (Check HTML report for statistics)"
fi
echo ""
echo "========================================="
echo "✅ All done!"
echo "========================================="
