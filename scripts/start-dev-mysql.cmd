@echo off
rem ===========================================================================
rem HFDC Bank - start a self-contained local MySQL 8 instance on port 33306.
rem The data directory lives inside the project (tools\mysql-test\data) and is
rem git-ignored. Root has NO password: local development only.
rem
rem If you prefer your normal MySQL service on 3306, skip this script and set
rem DB_URL / DB_USER / DB_PASSWORD before starting the backend.
rem ===========================================================================
setlocal
set "PORT=33306"
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin"
set "MYSQL_BASE=C:\Program Files\MySQL\MySQL Server 8.0"
set "DATADIR=%~dp0..\tools\mysql-test\data"
set "LOGFILE=%~dp0..\tools\mysql-test\server.log"

if not exist "%MYSQL_BIN%\mysqld.exe" (
  echo ERROR: mysqld.exe not found in "%MYSQL_BIN%".
  echo Edit MYSQL_BIN in this script or use your existing MySQL service.
  exit /b 1
)

powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort %PORT% -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if not errorlevel 1 (
  echo MySQL is already listening on port %PORT%.
  exit /b 0
)

if not exist "%DATADIR%\mysql" (
  echo Initialising a new MySQL data directory: %DATADIR%
  rem Remove a partial or uninitialised directory left by an earlier failure.
  if exist "%DATADIR%" rmdir /s /q "%DATADIR%"
  mkdir "%DATADIR%" 2>nul
  "%MYSQL_BIN%\mysqld.exe" --initialize-insecure --basedir="%MYSQL_BASE%" --datadir="%DATADIR%" --log-error="%LOGFILE%"
  if errorlevel 1 (
    echo ERROR: MySQL data directory initialisation failed. See %LOGFILE%
    exit /b 1
  )
)

start "HFDC MySQL" /min "%MYSQL_BIN%\mysqld.exe" --basedir="%MYSQL_BASE%" --datadir="%DATADIR%" --port=%PORT% --mysqlx=0 --log-error="%LOGFILE%"

powershell -NoProfile -Command "for ($i=0; $i -lt 40; $i++) { if (Get-NetTCPConnection -LocalPort %PORT% -State Listen -ErrorAction SilentlyContinue) { exit 0 }; Start-Sleep -Milliseconds 500 }; exit 1"
if errorlevel 1 (
  echo MySQL did not start. See %LOGFILE%
  exit /b 1
)

echo MySQL ready on 127.0.0.1:%PORT% (user root, no password - local dev only).
exit /b 0
