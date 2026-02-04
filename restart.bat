@echo off
echo ================================
echo   Restarting Application
echo ================================
echo.

echo Restarting containers...
docker-compose restart

echo.
echo Waiting for application...
timeout /t 10 /nobreak > nul

echo.
echo Application restarted successfully!
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.
