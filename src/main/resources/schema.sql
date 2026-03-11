-- ==========================================
-- easy_rent 表结构（rank2）
-- 说明：用于新库初始化
-- ==========================================

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    account_type VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '账号角色：USER/STAFF/ADMIN',
    display_name VARCHAR(80) NULL COMMENT '显示名（管理员/业务员）',
    phone VARCHAR(20) NULL COMMENT '联系电话（管理员/业务员）',
    enabled TINYINT(1) DEFAULT 1,
    can_publish TINYINT(1) DEFAULT 1,
    can_rent TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    INDEX idx_account_type (account_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一账号表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='普通用户资料扩展';

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
    assigned_staff_id BIGINT NULL COMMENT '分配业务员（accounts.id）',
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
        COMMENT 'PENDING_LANDLORD_APPROVAL/PENDING_STAFF_SIGNING/PENDING_ADMIN_APPROVAL/ACTIVE/EXPIRED/TERMINATED/REJECTED/TERMINATION_PENDING/TERMINATION_PENDING_COUNTERPARTY/TERMINATION_PENDING_STAFF_REVIEW/TERMINATION_FORCE_PENDING_JOINT_REVIEW',
    signed_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    assigned_staff_id BIGINT NULL COMMENT '签约跟进业务员（accounts.id）',
    signed_contract_url VARCHAR(500) NULL COMMENT '签约文件URL',
    signed_contract_name VARCHAR(255) NULL COMMENT '签约文件原名',
    signed_contract_uploaded_at DATETIME NULL,
    signed_contract_uploaded_by BIGINT NULL COMMENT '上传人（accounts.id）',
    landlord_termination_reject_count INT DEFAULT 0 COMMENT '房东发起终止被拒次数',
    tenant_termination_reject_count INT DEFAULT 0 COMMENT '租客发起终止被拒次数',
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
    review_staff_id BIGINT NULL COMMENT '终止审核业务员（accounts.id）',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/FORCE_TERMINATED',
    reason TEXT,
    force_reason TEXT,
    force_request TINYINT(1) DEFAULT 0,
    evidence_urls TEXT,
    counterparty_comment TEXT,
    staff_approved TINYINT(1) DEFAULT 0,
    admin_approved TINYINT(1) DEFAULT 0,
    staff_follow_up_plan TEXT,
    admin_decision_comment TEXT,
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
    sender_operator_id BIGINT NULL COMMENT '操作员发送者（accounts.id）',
    receiver_operator_id BIGINT NULL COMMENT '操作员接收者（accounts.id）',
    sender_operator_name VARCHAR(80) NULL,
    receiver_operator_name VARCHAR(80) NULL,
    title VARCHAR(200),
    content TEXT,
    message_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'UNREAD',
    related_contract_id BIGINT NULL,
    related_house_id BIGINT NULL,
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
    INDEX idx_msg_related_contract (related_contract_id),
    INDEX idx_msg_related_house (related_house_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息中心';

CREATE TABLE IF NOT EXISTS contract_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    request_id BIGINT NULL,
    file_type VARCHAR(40) NOT NULL COMMENT 'SIGNED_CONTRACT/TERMINATION_EVIDENCE/OTHER',
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) NULL,
    file_size BIGINT NULL,
    uploaded_by_account_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    INDEX idx_cf_contract (contract_id),
    INDEX idx_cf_request (request_id),
    INDEX idx_cf_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同文件元数据';

CREATE TABLE IF NOT EXISTS house_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    house_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) NULL,
    file_size BIGINT NULL,
    sort_order INT DEFAULT 0,
    is_cover TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE,
    INDEX idx_hi_house (house_id),
    INDEX idx_hi_sort (house_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源图片元数据';

CREATE TABLE IF NOT EXISTS termination_evidences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    contract_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) NULL,
    file_size BIGINT NULL,
    uploaded_by_account_id BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES termination_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
    INDEX idx_te_request (request_id),
    INDEX idx_te_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='终止证据元数据';
