@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo Gradle 8.9 is required. Please install Gradle 8.9 or run this project through Android Studio.
exit /b 1
