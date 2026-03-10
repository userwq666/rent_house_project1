-- ==========================================
-- easy_rent 数据库初始化脚本
-- ==========================================
-- 使用方式：
-- 1) mysql -u root -p < src/main/resources/init-database.sql
-- 2) 或在项目根目录执行：
--    Get-Content src/main/resources/init-database.sql | mysql -u root -p

DROP DATABASE IF EXISTS easy_rent;
CREATE DATABASE easy_rent
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE easy_rent;

SELECT '数据库 easy_rent 初始化完成' AS message;
