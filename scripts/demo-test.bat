@echo off
setlocal
cd /d "%~dp0.."
call run.bat --package A --test --repo-file test-repo\graph.txt --output examples\A.svg --reverse
if errorlevel 1 exit /b 1
call run.bat --package D --test --repo-file test-repo\graph.txt --output examples\D.svg
if errorlevel 1 exit /b 1
call run.bat --package G --test --repo-file test-repo\graph.txt --output examples\G.svg
endlocal
