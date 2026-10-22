@echo off
title Campus Lost & Found - Web Application
echo ====================================================
echo   Campus Lost & Found - Full Stack Web Portal
echo ====================================================
echo.

if not exist bin mkdir bin

echo [1/2] Compiling Java source files...
javac -d bin -sourcepath src src/com/campus/lostfound/Main.java src/com/campus/lostfound/web/ServerMain.java
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed! Ensure Java JDK 8 or newer is installed and on your PATH.
    pause
    exit /b %errorlevel%
)

echo [2/2] Starting Web Server on http://localhost:8080 ...
echo.
echo Opening browser in 2 seconds...
start "" http://localhost:8080

java -cp bin com.campus.lostfound.web.ServerMain 8080
pause
