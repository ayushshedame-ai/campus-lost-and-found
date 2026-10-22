#!/usr/bin/env bash
# Campus Lost & Found - Launcher Script for Linux / macOS
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

mkdir -p bin

echo "===================================================="
echo "  Campus Lost & Found Management System"
echo "===================================================="
echo "Compiling Java files..."
javac -d bin $(find src -name "*.java")

echo ""
echo "Select mode:"
echo "  1) Launch Full-Stack Web App (http://localhost:8080) [Recommended]"
echo "  2) Launch Console Menu"
read -p "Enter choice [1 or 2, default 1]: " choice

if [ "$choice" == "2" ]; then
    java -cp bin com.campus.lostfound.Main --console
else
    echo "Starting web server at http://localhost:8080 ..."
    # Try to open in default browser
    if which xdg-open > /dev/null; then
        xdg-open http://localhost:8080 &
    elif which open > /dev/null; then
        open http://localhost:8080 &
    fi
    java -cp bin com.campus.lostfound.web.ServerMain 8080
fi
