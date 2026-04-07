@echo off
echo Starting Ghost Protocol v2.0 Backend...

:: Check if Maven is available in the current environment
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [WARNING] Maven is not in PATH! Assuming the temporary env is used or we will fallback to downloading.
    :: Since this script is simple, we will just use the exact path we known is working if it exists
    if exist "%TEMP%\maven\apache-maven-3.9.6\bin\mvn.cmd" (
        set "MVN_CMD=%TEMP%\maven\apache-maven-3.9.6\bin\mvn.cmd"
    ) else (
        echo [ERROR] Maven not found. Please install Maven and add 'mvn' to your PATH.
        pause
        exit /b 1
    )
) else (
    set "MVN_CMD=mvn"
)

:: Set the absolute path so this script works from anywhere (e.g. the Desktop)
set "PROJECT_ROOT=C:\Users\rohit\.gemini\antigravity\playground\dynamicproj\ghost-in-the-bytecode"

:: Ensure we are in the correct directory
cd /d "%PROJECT_ROOT%"

:: Start the backend silently in a new window so the user doesn't have to keep this background prompt open
echo Launching Spring Boot API Server in port 8080...
start "Ghost Server" cmd /c "cd backend\api && %MVN_CMD% spring-boot:run"

:: Wait 1 second to ensure the command initializes
timeout /t 1 > nul

:: Open the index.html page in the default browser. 
echo Opening Web UI...
start "" "file:///%PROJECT_ROOT%\frontend\index.html"

echo Process launched. You can close this window.
exit
