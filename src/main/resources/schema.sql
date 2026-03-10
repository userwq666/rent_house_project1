-- 数据库初始化脚本
-- 注意：此文件在数据库已存在时执行，用于创建表结构
-- 但由于使用JPA自动建表，此文件作为备份参考

-- 创建管理员表
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '管理员用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT '角色（ADMIN/ROOT）',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    last_login_time DATETIME COMMENT '最后登录时间',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 创建普通用户表（租客）
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    id_card VARCHAR(20) COMMENT '身份证号',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户表';

-- 创建房主表
CREATE TABLE IF NOT EXISTS landlords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '房主用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    id_card VARCHAR(20) COMMENT '身份证号',
    address TEXT COMMENT '联系地址',
    house_count INT DEFAULT 0 COMMENT '房源数量',
    credit_score DECIMAL(10,2) DEFAULT 0.00 COMMENT '信用评分',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_credit_score (credit_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房主表';

-- 创建房屋信息表
CREATE TABLE IF NOT EXISTS houses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    landlord_id BIGINT NOT NULL COMMENT '房主ID',
    title VARCHAR(200) NOT NULL COMMENT '房源标题',
    description TEXT COMMENT '房源描述',
    address VARCHAR(255) NOT NULL COMMENT '详细地址',
    district VARCHAR(100) COMMENT '所属区域',
    house_type VARCHAR(50) NOT NULL COMMENT '户型（如：两室一厅）',
    area DECIMAL(10,2) COMMENT '面积（平方米）',
    floor INT COMMENT '楼层',
    total_floor INT COMMENT '总楼层',
    rent_price DECIMAL(10,2) NOT NULL COMMENT '月租金',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态（AVAILABLE/RENTED/MAINTENANCE/OFFLINE）',
    direction VARCHAR(50) COMMENT '朝向',
    facilities TEXT COMMENT '配套设施',
    images TEXT COMMENT '房源图片',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    FOREIGN KEY (landlord_id) REFERENCES landlords(id) ON DELETE CASCADE,
    INDEX idx_landlord_id (landlord_id),
    INDEX idx_status (status),
    INDEX idx_district (district),
    INDEX idx_rent_price (rent_price),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房屋信息表';






