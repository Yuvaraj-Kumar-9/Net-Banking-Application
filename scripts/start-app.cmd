@echo off
rem ===========================================================================
rem HFDC Bank - one-click local start (Windows).
rem
rem 1. Starts the isolated MySQL 8 instance on port 33306 if it is not running.
rem 2. Runs the packaged Spring Boot jar, which serves BOTH the API and the
rem    React website on http://localhost:8080.
rem
rem Any DB_URL / DB_USER / DB_PASSWORD / JWT_SECRET already set in your
rem environment wins over the local development defaults below.
rem ===========================================================================
setlocal
set "ROOT=%~dp0.."
cd /d "%ROOT%"

if not exist "target\hfdc-bank.jar" (
  echo.
  echo ERROR: target\hfdc-bank.jar was not found.
  echo Build it first with:
  echo   cd frontend
  echo   npm ci
  echo   npm run build
  echo   cd ..
  echo   mvn -Pfrontend -DskipTests package
  echo.
  pause
  exit /b 1
)

call "%ROOT%\scripts\start-dev-mysql.cmd"
if errorlevel 1 (
  echo.
  echo ERROR: the local MySQL instance could not be started.
  echo See tools\mysql-test\server.log for details.
  echo.
  pause
  exit /b 1
)

rem First run on a brand-new data directory: create the schema, demo data and
rem the hfdc_user login. An existing hfdc_bank database is never modified.
if not exist "%ROOT%\tools\mysql-test\data\hfdc_bank" (
  echo.
  echo First run detected - creating the hfdc_bank schema and demo data ...
  call "%ROOT%\scripts\dev-db-reset.cmd"
  if errorlevel 1 (
    echo.
    echo ERROR: the database could not be created.
    echo.
    pause
    exit /b 1
  )
)

if not defined DB_URL set "DB_URL=jdbc:mysql://127.0.0.1:33306/hfdc_bank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8"
if not defined DB_USER set "DB_USER=hfdc_user"
if not defined DB_PASSWORD set "DB_PASSWORD=change_me"
if not defined JWT_SECRET set "JWT_SECRET=local-dev-only-hfdc-bank-secret-change-me-1234567890"
if not defined DEV_MODE set "DEV_MODE=true"

echo.
echo ============================================================
echo  HFDC Bank is starting.
echo.
echo  Website : http://localhost:8080
echo  Swagger : http://localhost:8080/swagger-ui.html
echo  Login   : aarav@hfdcbank.test / Demo@1234  (PIN 1234)
echo.
echo  Keep this window open. Press Ctrl+C to stop the website.
echo ============================================================
echo.

java -jar target\hfdc-bank.jar

echo.
echo The application has stopped.
pause
endlocal
