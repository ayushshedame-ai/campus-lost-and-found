@echo off
title Campus Lost & Found - Console Menu
echo ====================================================
echo   Campus Lost & Found - Interactive Console Menu
echo ====================================================
echo.

if not exist bin mkdir bin

echo Compiling Java source files...
javac -d bin -sourcepath src src/com/campus/lostfound/Main.java
if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed! Ensure Java JDK 8 or newer is installed and on your PATH.
    pause
    exit /b %errorlevel%
)

echo Starting Console Application...
echo.
java -cp bin com.campus.lostfound.Main --console
pause
