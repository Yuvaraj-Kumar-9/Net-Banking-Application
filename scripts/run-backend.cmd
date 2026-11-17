@echo off
rem ===========================================================================
rem HFDC Bank - run the Spring Boot backend against the local dev database.
rem Requires: JDK 21, Maven 3.9+ (set MAVEN_HOME if mvn is not on PATH).
rem Any value already present in your environment wins over these dev defaults.
rem ===========================================================================
setlocal
set "ROOT=%~dp0.."
if not defined DB_URL set "DB_URL=jdbc:mysql://127.0.0.1:33306/hfdc_bank?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8"
if not defined DB_USER set "DB_USER=hfdc_user"
if not defined DB_PASSWORD set "DB_PASSWORD=change_me"
if not defined JWT_SECRET set "JWT_SECRET=local-dev-only-hfdc-bank-secret-change-me-1234567890"
if not defined CORS_ALLOWED_ORIGINS set "CORS_ALLOWED_ORIGINS=http://localhost:5173"
if not defined DEV_MODE set "DEV_MODE=true"

cd /d "%ROOT%"
if "%MAVEN_HOME%"=="" (
  mvn spring-boot:run
) else (
  call "%MAVEN_HOME%\bin\mvn.cmd" spring-boot:run
)
