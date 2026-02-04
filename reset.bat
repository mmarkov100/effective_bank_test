@echo off
echo ================================
echo   FULL RESET (DB will be deleted)
echo ================================
echo.

set /p confirm="Are you sure? All data will be lost! (y/n): "
if /i not "%confirm%"=="y" (
    echo Reset cancelled.
    exit /b
)

echo.
echo [1/3] Stopping and removing containers...
docker-compose down -v

echo.
echo [2/3] Rebuilding and starting...
docker-compose up --build -d

echo.
echo [3/3] Waiting for application...
timeout /t 20 /nobreak > nul

echo.
echo ================================
echo   Reset completed!
echo ================================
echo.
echo Database recreated with migrations applied.
echo Default admin: admin / admin123
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.
