@echo off
REM
REM k6 캐시 성능 비교 테스트 실행 스크립트 (Windows)
REM
REM 사용법:
REM   run-comparison-test.bat
REM

setlocal

set BASE_URL=http://localhost:8080
set RESULTS_DIR=.\results

REM 결과 디렉토리 생성
if not exist %RESULTS_DIR% mkdir %RESULTS_DIR%

echo ========================================
echo   k6 캐시 성능 비교 테스트
echo ========================================
echo   Base URL: %BASE_URL%
echo   Results: %RESULTS_DIR%
echo ========================================
echo.

REM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
REM 1. 캐시 비우기 (Cold Start 테스트 준비)
REM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo [Step 1] 캐시 비우기...
curl -X DELETE "%BASE_URL%/api/admin/cache/all" -s
echo.
timeout /t 2 /nobreak > nul

REM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
REM 2. Cold Cache 테스트 (캐시 적용 전 시뮬레이션)
REM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo [Step 2] Cold Cache 테스트 (워밍업 없음)...
k6 run --env BASE_URL=%BASE_URL% --env SCENARIO=light --env WARMUP=false cache-performance-test.js

echo.
echo 캐시 상태 확인:
curl -s "%BASE_URL%/api/admin/cache/summary"
echo.
echo.

REM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
REM 3. Warm Cache 테스트 (캐시 적용 후)
REM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo [Step 3] Warm Cache 테스트 (워밍업 포함)...
k6 run --env BASE_URL=%BASE_URL% --env SCENARIO=light --env WARMUP=true cache-performance-test.js

echo.
echo 최종 캐시 상태:
curl -s "%BASE_URL%/api/admin/cache/summary"
echo.

REM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
REM 4. 결과 비교 출력
REM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo ========================================
echo   테스트 완료!
echo ========================================
echo.
echo 결과 파일:
dir %RESULTS_DIR%
echo.
echo ========================================

endlocal
pause
