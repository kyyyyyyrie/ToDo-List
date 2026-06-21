@echo off
set DIR=%~dp0
set GRADLE_VERSION=8.7
set GRADLE_DIR=%DIR%.gradle\gradle-%GRADLE_VERSION%

if not exist "%GRADLE_DIR%\bin\gradle.bat" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%DIR%.gradle\gradle.zip'; Expand-Archive -LiteralPath '%DIR%.gradle\gradle.zip' -DestinationPath '%DIR%.gradle'; Remove-Item -LiteralPath '%DIR%.gradle\gradle.zip' -Force"
)

call "%GRADLE_DIR%\bin\gradle.bat" %*
