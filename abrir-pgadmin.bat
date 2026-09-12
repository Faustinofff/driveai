@echo off
chcp 65001 >nul
title DriveAI - pgAdmin 4
set PGDATA=C:\driveai\.tools\pgdata
set PGBIN=C:\driveai\.tools\pgsql\bin

"%PGBIN%\pg_ctl" -D "%PGDATA%" status >nul 2>&1
if %errorlevel%==0 (
  echo [OK] PostgreSQL ya esta corriendo en el puerto 5433.
) else (
  echo [..] Arrancando PostgreSQL en el puerto 5433...
  "%PGBIN%\pg_ctl" -D "%PGDATA%" -l "%PGDATA%\pg.log" -o "-p 5433 -c listen_addresses=localhost" start >nul
)

echo [..] Abriendo pgAdmin 4...
start "" "C:\driveai\.tools\pgAdmin 4\runtime\pgAdmin4.exe"
echo [OK] Listo.