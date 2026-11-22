@echo off
set "SCRIPT_DIR=%~dp0"
set "JAVA_HOME=%SCRIPT_DIR%.gradle-home\jdks\azul_systems__inc_-8-amd64-windows.2"
set "GRADLE_USER_HOME=%SCRIPT_DIR%.gradle-home"

call "%SCRIPT_DIR%gradlew.bat" %*
