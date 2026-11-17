@echo off
rem ===========================================================================
rem HFDC Bank - recreate the local development database on port 33306.
rem The isolated MySQL instance must already be running (see start-dev-mysql.cmd).
rem WARNING: drops the hfdc_bank database and all of its data.
rem ===========================================================================
setlocal
set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set "ROOT=%~dp0.."
set "PORT=33306"
if not defined HFDC_DB_PASSWORD set "HFDC_DB_PASSWORD=change_me"

echo Dropping and recreating hfdc_bank on 127.0.0.1:%PORT% ...
"%MYSQL%" -h 127.0.0.1 -P %PORT% -u root -e "DROP DATABASE IF EXISTS hfdc_bank; CREATE DATABASE hfdc_bank CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 goto :error

"%MYSQL%" -h 127.0.0.1 -P %PORT% -u root hfdc_bank < "%ROOT%\sql\01_schema.sql"
if errorlevel 1 goto :error

"%MYSQL%" -h 127.0.0.1 -P %PORT% -u root hfdc_bank < "%ROOT%\sql\02_seed.sql"
if errorlevel 1 goto :error

rem Local-only application user. Override HFDC_DB_PASSWORD before running this
rem script if you want a different password; run-backend.cmd must match it.
"%MYSQL%" -h 127.0.0.1 -P %PORT% -u root -e "CREATE USER IF NOT EXISTS 'hfdc_user'@'localhost' IDENTIFIED BY '%HFDC_DB_PASSWORD%'; CREATE USER IF NOT EXISTS 'hfdc_user'@'%%' IDENTIFIED BY '%HFDC_DB_PASSWORD%'; ALTER USER 'hfdc_user'@'localhost' IDENTIFIED BY '%HFDC_DB_PASSWORD%'; ALTER USER 'hfdc_user'@'%%' IDENTIFIED BY '%HFDC_DB_PASSWORD%'; GRANT ALL PRIVILEGES ON hfdc_bank.* TO 'hfdc_user'@'localhost'; GRANT ALL PRIVILEGES ON hfdc_bank.* TO 'hfdc_user'@'%%'; FLUSH PRIVILEGES;"
if errorlevel 1 goto :error

echo.
echo Database ready. Demo logins:
echo   aarav@hfdcbank.test / Demo@1234  (PIN 1234)
echo   priya@hfdcbank.test / Demo@1234  (PIN 4321)
exit /b 0

:error
echo.
echo Database setup FAILED. Check that MySQL is listening on port %PORT%.
exit /b 1
