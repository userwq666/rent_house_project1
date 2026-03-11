-- ==========================================
-- easy_rent 数据库初始化脚本
-- ==========================================
-- 使用方式（命令行推荐）：
--   1) mysql -u root -p < src/main/resources/init-database.sql
--   2) 启动后端后，应用会自动执行 schema-v3.sql 建表并初始化默认管理员账号
--      默认管理员：admin / 123456
--
-- 若在 IDE 的数据库工具执行，请分两步：
--   A. 先执行本文件（仅重建 easy_rent 数据库）
--   B. 再执行 src/main/resources/schema-v3.sql

DROP DATABASE IF EXISTS easy_rent;
CREATE DATABASE easy_rent
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE easy_rent;

SELECT '数据库 easy_rent 初始化完成' AS message;
