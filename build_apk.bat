@echo off
echo Собираю APK WebView Preview...
cd /d "%~dp0"
call gradlew assembleRelease 2>&1 | findstr /I "BUILD SUCCESSFUL" || echo ОШИБКА: Проверь Android Studio / SDK
pause