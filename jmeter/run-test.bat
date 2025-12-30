@echo off
REM JMeter Auth API Load Test Runner (Windows)
REM Usage: jmeter\run-test.bat

echo =========================================
echo JMeter Auth API Load Test
echo =========================================

REM 1. 사전 체크
echo.
echo [1/5] Checking prerequisites...

REM Docker 컨테이너 확인
docker-compose ps | findstr "Up" >nul 2>&1
if errorlevel 1 (
    echo X Error: Docker containers are not running
    echo Run: docker-compose up -d
    exit /b 1
)
echo + Docker containers are running

REM 애플리케이션 확인
curl -s http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    echo X Error: Application is not running
    echo Run: gradlew.bat bootRun
    exit /b 1
)
echo + Application is running

REM 2. 테스트 사용자 생성
echo.
echo [2/5] Creating test user...
curl -X POST http://localhost:8080/api/v1/auth/signup ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"test@example.com\",\"password\":\"Test1234!@\",\"name\":\"테스트유저\",\"phone\":\"010-1234-5678\"}" ^
  -w "\nHTTP Status: %%{http_code}\n" ^
  -s
echo i User may already exist (OK for login test)

REM 3. 결과 디렉토리 정리
echo.
echo [3/5] Preparing result directories...
if exist jmeter\results rmdir /s /q jmeter\results
if exist jmeter\reports rmdir /s /q jmeter\reports
mkdir jmeter\results
mkdir jmeter\reports
echo + Directories ready

REM 4. JMeter 테스트 실행
echo.
echo [4/5] Running JMeter load test...
echo Test scenarios:
echo   - Signup: 100 threads x 10 loops = 1,000 requests
echo   - Login:  200 threads x 20 loops = 4,000 requests
echo.

jmeter.bat -n -t jmeter\auth-load-test.jmx ^
  -l jmeter\results\auth-load-test-results.jtl ^
  -e -o jmeter\reports\auth-load-test-report

if errorlevel 1 (
    echo X Error: JMeter test failed
    exit /b 1
)

echo.
echo + Test completed!

REM 5. 결과 출력
echo.
echo [5/5] Test Results
echo =========================================
echo.
echo HTML Report:
echo   file:///%CD%\jmeter\reports\auth-load-test-report\index.html
echo.
echo Raw Results:
echo   %CD%\jmeter\results\auth-load-test-results.jtl
echo.
echo =========================================
echo + All done!
echo =========================================
echo.
echo Opening HTML report in browser...
start jmeter\reports\auth-load-test-report\index.html
