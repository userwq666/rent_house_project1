-- ==========================================
-- easy_rent 表结构（v3）
-- 说明：该脚本用于“新库初始化”，不包含 ALTER 增量语句
-- ==========================================

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    account_type VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '账号类型：USER',
    enabled TINYINT(1) DEFAULT 1,
    can_publish TINYINT(1) DEFAULT 1,
    can_rent TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    INDEX idx_account_type (account_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='普通用户账号';

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT UNIQUE NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    id_card VARCHAR(20),
    avatar VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    INDEX idx_phone (phone),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='普通用户信息';

CREATE TABLE IF NOT EXISTS operator_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'ADMIN/STAFF',
    display_name VARCHAR(80),
    phone VARCHAR(20),
    enabled TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_operator_role (role),
    INDEX idx_operator_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作员账号';

CREATE TABLE IF NOT EXISTS houses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    address VARCHAR(255) NOT NULL,
    district VARCHAR(100),
    house_type VARCHAR(50) NOT NULL,
    area DECIMAL(10,2),
    floor INT,
    rent_price DECIMAL(10,2) NOT NULL,
    deposit DECIMAL(10,2),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_STAFF_REVIEW'
        COMMENT 'PENDING_STAFF_REVIEW/AVAILABLE/PENDING/RENTED/OFFLINE',
    description TEXT,
    images MEDIUMTEXT,
    facilities TEXT,
    view_count INT DEFAULT 0,
    assigned_staff_id BIGINT NULL COMMENT '分配业务员ID',
    review_comment TEXT NULL COMMENT '审核意见',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status),
    INDEX idx_district (district),
    INDEX idx_rent_price (rent_price),
    INDEX idx_assigned_staff_id (assigned_staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源';

CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    house_id BIGINT NOT NULL,
    landlord_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    rent_price DECIMAL(10,2) NOT NULL,
    deposit DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_LANDLORD_APPROVAL'
        COMMENT 'PENDING_LANDLORD_APPROVAL/PENDING_STAFF_SIGNING/PENDING_ADMIN_APPROVAL/ACTIVE/EXPIRED/TERMINATED/TERMINATION_PENDING/TERMINATION_PENDING_STAFF_REVIEW',
    signed_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    assigned_staff_id BIGINT NULL COMMENT '签约跟进业务员',
    signed_contract_url VARCHAR(500) NULL COMMENT '签约文件URL',
    signed_contract_name VARCHAR(255) NULL COMMENT '签约文件原名',
    signed_contract_uploaded_at DATETIME NULL,
    signed_contract_uploaded_by BIGINT NULL COMMENT '上传业务员ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_house_id (house_id),
    INDEX idx_landlord_id (landlord_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_contract_status (status),
    INDEX idx_assigned_staff_id (assigned_staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同';

CREATE TABLE IF NOT EXISTS termination_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    responder_id BIGINT NULL,
    review_staff_id BIGINT NULL COMMENT '终止审核业务员',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/FORCE_TERMINATED',
    reason TEXT,
    force_reason TEXT,
    admin_notified TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (responder_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tr_contract (contract_id),
    INDEX idx_tr_status (status),
    INDEX idx_tr_review_staff_id (review_staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同终止申请';

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NULL COMMENT '用户发送者',
    receiver_id BIGINT NULL COMMENT '用户接收者',
    sender_operator_id BIGINT NULL COMMENT '操作员发送者',
    receiver_operator_id BIGINT NULL COMMENT '操作员接收者',
    sender_operator_name VARCHAR(80) NULL,
    receiver_operator_name VARCHAR(80) NULL,
    title VARCHAR(200),
    content TEXT,
    message_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'UNREAD',
    related_contract_id BIGINT NULL,
    related_request_id BIGINT NULL,
    require_action TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    archived_by_user_ids VARCHAR(500) NULL,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_msg_sender (sender_id),
    INDEX idx_msg_receiver (receiver_id),
    INDEX idx_msg_sender_operator (sender_operator_id),
    INDEX idx_msg_receiver_operator (receiver_operator_id),
    INDEX idx_msg_status (status),
    INDEX idx_msg_related_contract (related_contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息中心';

