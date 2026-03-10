-- ==========================================
-- 租房系统数据表 v3.0（精简版 + 索引增强）
-- ==========================================

-- 1. 系统账号表
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    account_type VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '账号类型：ADMIN/USER',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    can_publish TINYINT(1) DEFAULT 1 COMMENT '是否允许发布房源',
    can_rent TINYINT(1) DEFAULT 1 COMMENT '是否允许租赁',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    INDEX idx_account_type (account_type),
    INDEX idx_enabled (enabled),
    INDEX idx_account_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统账号表';

-- 2. 用户信息表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT UNIQUE NOT NULL COMMENT '账号ID',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    id_card VARCHAR(20) COMMENT '身份证号',
    avatar VARCHAR(255) COMMENT '头像URL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    INDEX idx_account_id (account_id),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_user_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 3. 房屋表
CREATE TABLE IF NOT EXISTS houses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL COMMENT '房主ID',
    title VARCHAR(200) NOT NULL COMMENT '房源标题',
    address VARCHAR(255) NOT NULL COMMENT '详细地址',
    district VARCHAR(100) COMMENT '所属区域',
    house_type VARCHAR(50) NOT NULL COMMENT '户型（如：两室一厅）',
    area DECIMAL(10,2) COMMENT '面积（平方米）',
    floor INT COMMENT '楼层',
    rent_price DECIMAL(10,2) NOT NULL COMMENT '月租金',
    deposit DECIMAL(10,2) COMMENT '押金',
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/RENTED/OFFLINE/PENDING',
    description TEXT COMMENT '房源描述',
    images MEDIUMTEXT COMMENT '房源图片（多个URL/Base64，逗号分隔）',
    facilities TEXT COMMENT '配套设施',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status),
    INDEX idx_district (district),
    INDEX idx_rent_price (rent_price),
    INDEX idx_house_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋表';

-- 4. 租房合同表
CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    house_id BIGINT NOT NULL COMMENT '房屋ID',
    landlord_id BIGINT NOT NULL COMMENT '房主ID',
    tenant_id BIGINT NOT NULL COMMENT '租客ID',
    rent_price DECIMAL(10,2) NOT NULL COMMENT '实际月租金',
    deposit DECIMAL(10,2) NOT NULL COMMENT '押金',
    start_date DATE NOT NULL COMMENT '租期开始日期',
    end_date DATE NOT NULL COMMENT '租期结束日期',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT '合同状态：ACTIVE/EXPIRED/TERMINATED/TERMINATION_PENDING/PENDING_LANDLORD_APPROVAL/PENDING_ADMIN_APPROVAL',
    signed_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '签约时间',
    notes TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_house_id (house_id),
    INDEX idx_landlord_id (landlord_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_contract_status (status),
    INDEX idx_contract_dates (start_date, end_date),
    INDEX idx_contract_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租房合同表';

-- 5. 合同终止申请
CREATE TABLE IF NOT EXISTS termination_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL COMMENT '合同ID',
    requester_id BIGINT NOT NULL COMMENT '申请方ID',
    responder_id BIGINT NOT NULL COMMENT '确认方ID',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/FORCE_TERMINATED',
    reason TEXT COMMENT '终止原因',
    force_reason TEXT COMMENT '强制终止原因',
    admin_notified TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (responder_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tr_contract (contract_id),
    INDEX idx_tr_requester (requester_id),
    INDEX idx_tr_responder (responder_id),
    INDEX idx_tr_status (status),
    INDEX idx_tr_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同终止申请';

-- 6. 系统消息
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NULL COMMENT '发送人',
    receiver_id BIGINT NULL COMMENT '接收人',
    title VARCHAR(200),
    content TEXT,
    message_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'UNREAD',
    related_contract_id BIGINT NULL,
    related_request_id BIGINT NULL,
    require_action TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    archived_by_user_ids VARCHAR(500) NULL COMMENT '已归档此消息的用户 ID 列表（逗号分隔）',
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_msg_sender (sender_id),
    INDEX idx_msg_receiver (receiver_id),
    INDEX idx_msg_status (status),
    INDEX idx_msg_created (created_at),
    INDEX idx_msg_related_contract (related_contract_id),
    INDEX idx_msg_related_request (related_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息';
