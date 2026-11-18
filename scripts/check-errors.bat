@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
call run.bat --repository https://example.org >nul 2>&1
if not errorlevel 2 goto fail
call run.bat --package "bad name" --repository https://example.org >nul 2>&1
if not errorlevel 2 goto fail
call run.bat --package A >nul 2>&1
if not errorlevel 2 goto fail
call run.bat --package A --repository invalid-url >nul 2>&1
if not errorlevel 2 goto fail
call run.bat --package A --test --repo-file missing.txt >nul 2>&1
if not errorlevel 2 goto fail
call run.bat --package A --test --repo-file test-repo\graph.txt --output graph.png >nul 2>&1
if not errorlevel 2 goto fail
echo [OK] Ошибки аргументов обработаны.
exit /b 0
:fail
echo [FAIL] Ожидалась ошибка параметров.
exit /b 1
