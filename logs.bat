@echo off
echo ================================
echo   Application Logs
echo ================================
echo.
echo Press Ctrl+C to exit
echo.
docker-compose logs -f app
