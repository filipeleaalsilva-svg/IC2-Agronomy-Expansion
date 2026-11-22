@echo off
set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.5.10002-hotspot"
set "GRADLE_USER_HOME=%ROOT_DIR%\.gradle-home"

call "%ROOT_DIR%\gradlew.bat" -p "%SCRIPT_DIR%." %*
