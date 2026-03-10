-- ==========================================
-- 数据库初始化脚本 v3.0（修复版）
-- ==========================================
-- 用途：删除旧数据库并创建新数据库
-- 修复内容：
--   1. houses.status 字段长度增加到 VARCHAR(50)
--   2. messages.archived_by_user_ids 字段（用于归档功能）
-- 使用方法：
--   1. Windows: Get-Content src\main\resources\init-database.sql | mysql -u root -p
--   2. Linux/Mac: mysql -u root -p < src/main/resources/init-database.sql
-- ==========================================

-- 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS rent_house_db;

-- 创建新数据库
CREATE DATABASE rent_house_db 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 切换到新数据库
USE rent_house_db;

-- 执行表结构创建脚本
SOURCE src/main/resources/schema-v3.sql;

-- 显示成功信息
SELECT '数据库 rent_house_db 创建成功！' AS message;
SELECT '表结构已包含以下修复：' AS fixes;
SELECT '  1. houses.status 字段长度：VARCHAR(50)' AS fix1;
SELECT '  2. messages.archived_by_user_ids 字段：VARCHAR(500)' AS fix2;
SELECT '请启动 Spring Boot 应用以完成初始化。' AS next_step;
