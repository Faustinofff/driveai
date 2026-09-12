@echo off
chcp 65001 >nul
cd /d C:\driveai
setlocal
set PGPASSWORD=cardb_pass
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -p 5433 -U cardb_user -d cardb -P pager=off