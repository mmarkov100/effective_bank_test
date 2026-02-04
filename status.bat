@echo off
echo ================================
echo   Container Status
echo ================================
echo.
docker-compose ps
echo.
echo ================================
echo   Application Health
echo ================================
echo.
curl -s http://localhost:8080/actuator/health
echo.
echo.
