@echo off
rem ===========================================================================
rem HFDC Bank - stop the backend and the isolated local MySQL instance.
rem Run this from a second Command Prompt, or after closing the start window.
rem ===========================================================================
setlocal
set "PORT=33306"
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin"

echo Stopping the HFDC Bank backend ...
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*hfdc-bank.jar*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }"

powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort %PORT% -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if errorlevel 1 (
  echo MySQL is not running on port %PORT%.
  goto :done
)

echo Stopping MySQL on port %PORT% ...
"%MYSQL_BIN%\mysqladmin.exe" -h 127.0.0.1 -P %PORT% -u root shutdown
if errorlevel 1 (
  echo WARNING: MySQL did not stop cleanly. See tools\mysql-test\server.log
)

:done
echo.
echo Done.
ping -n 2 127.0.0.1 >nul
endlocal
