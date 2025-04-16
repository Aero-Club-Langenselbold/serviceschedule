@echo off

cd schedule-api

echo Running Maven build...
call mvn clean package
if %ERRORLEVEL% NEQ 0 (
    echo Maven build failed. Exiting.
    exit /b %ERRORLEVEL%
)

cd ..

cd schedule-ui

echo Running pnpm build...
call pnpm dlx nuxi generate --dotenv .env.production
if %ERRORLEVEL% NEQ 0 (
    echo Pnpm build failed. Exiting.
    exit /b %ERRORLEVEL%
)

echo Starting Docker Compose...
call docker compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose failed to start. Exiting.
    exit /b %ERRORLEVEL%
)

echo Build and deployment completed successfully.