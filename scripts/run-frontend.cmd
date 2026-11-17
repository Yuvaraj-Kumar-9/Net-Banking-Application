@echo off
rem ===========================================================================
rem HFDC Bank - install dependencies (first run) and start the Vite dev server.
rem ===========================================================================
setlocal
cd /d "%~dp0..\frontend"
if not exist node_modules (
  echo Installing frontend dependencies...
  call npm install
  if errorlevel 1 exit /b 1
)
call npm run dev
