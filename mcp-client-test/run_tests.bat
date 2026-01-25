@echo off
REM MCP Test Client Runner
REM Run this batch file to execute all MCP tests

echo ====================================
echo MCP Test Client Runner
echo ====================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python 3.8+ from https://www.python.org/
    pause
    exit /b 1
)

REM Install dependencies if needed
echo Installing dependencies...
pip install -q mcp aiohttp 2>nul

REM Run the tests
echo.
echo Running MCP tests...
echo.
python mcp_test_client.py

echo.
echo ====================================
echo Tests complete!
echo ====================================
pause
