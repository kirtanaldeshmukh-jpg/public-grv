@echo off
setlocal enabledelayedexpansion
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.4.1"
set /p CP=<"%~dp0backend\extracted_cp.txt"
set "CLASSPATH=%~dp0backend\target\classes;!CP!"
"%JAVA_HOME%\bin\java.exe" -Dspring.profiles.active=dev com.resolvetrack.ResolveTrackApplication

