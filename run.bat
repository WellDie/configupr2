@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
if not exist build mkdir build
dir /s /b src\main\java\*.java > build\sources.txt
javac -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1
java -cp build\classes ru.mirea.cargograph.Main %*
endlocal
