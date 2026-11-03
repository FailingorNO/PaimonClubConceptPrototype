@echo off
chcp 65001 >nul
rem ============================================================
rem 派蒙社一键建库：创建 paimonclub 库、25 张表、系统基础数据（唯一管理员 + 功能字典，无示例内容）
rem 用法：scripts\init-db.bat [用户名] [密码] [主机] [端口]
rem 默认：root / 123456 / localhost / 3306
rem 注意：脚本会先 DROP 同名表再重建，库内旧数据会被清空
rem ============================================================
setlocal

set DB_USER=%~1
if "%DB_USER%"=="" set DB_USER=root
set DB_PASS=%~2
if "%DB_PASS%"=="" set DB_PASS=123456
set DB_HOST=%~3
if "%DB_HOST%"=="" set DB_HOST=127.0.0.1
set DB_PORT=%~4
if "%DB_PORT%"=="" set DB_PORT=3306

set SCRIPT_DIR=%~dp0
set SQL_FILE=%SCRIPT_DIR%..\sql\init-paimonclub.sql

if not exist "%SQL_FILE%" (
  echo [init-db] 找不到建库脚本：%SQL_FILE%
  exit /b 1
)

set MYSQL_EXE=mysql
where mysql >nul 2>nul
if errorlevel 1 (
  if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
  ) else (
    echo [init-db] 未找到 mysql 客户端，请把 MySQL bin 目录加入 PATH
    exit /b 1
  )
)

echo [init-db] 导入 %SQL_FILE% 到 %DB_HOST%:%DB_PORT% （用户 %DB_USER%）
set MYSQL_PWD=%DB_PASS%
"%MYSQL_EXE%" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% --default-character-set=utf8mb4 < "%SQL_FILE%"
if errorlevel 1 (
  echo [init-db] 导入失败，请检查账号密码与 MySQL 服务状态
  exit /b 1
)

echo [init-db] 完成。默认管理员：admin / admin123（唯一固定账号）
endlocal
