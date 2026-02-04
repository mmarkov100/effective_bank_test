@echo off
echo ================================
echo   START PROGRAM
echo ================================
echo.

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
echo   Start completed!
echo ================================
echo.
echo Database recreated with migrations applied.
echo Default admin: admin / admin123
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.
