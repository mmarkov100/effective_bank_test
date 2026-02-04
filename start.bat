@echo off
echo ================================
echo   Starting Bank Application
echo ================================
echo.

echo [1/3] Stopping existing containers...
docker-compose down

echo.
echo [2/3] Building and starting containers...
docker-compose up --build -d

echo.
echo [3/3] Waiting for application to start...
timeout /t 15 /nobreak > nul

echo.
echo ================================
echo   Application is ready!
echo ================================
echo.
echo Swagger UI:  http://localhost:8080/swagger-ui.html
echo PostgreSQL:  localhost:5433
echo.
echo Default admin credentials:
echo   Username: admin
echo   Password: admin123
echo.
echo Useful commands:
echo   View logs:  logs.bat
echo   Restart:    restart.bat
echo   Reset DB:   reset.bat
echo   Stop:       stop.bat
echo   Status:     status.bat
echo.
