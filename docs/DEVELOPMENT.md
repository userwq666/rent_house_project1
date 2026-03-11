# 开发说明

## 1. 运行要求

- JDK 21
- Node.js 18+
- MySQL 5.7+

## 2. 启动流程

### 2.1 初始化数据库

```bash
mysql -u root -p < src/main/resources/init-database.sql
```

### 2.2 启动后端

```bash
mvn spring-boot:run
```

- 启动时执行 `schema-v3.sql`（`spring.sql.init.mode=always`）。
- `DataInitializer` 会自动补齐默认管理员：`admin / 123456`。

### 2.3 启动前端

```bash
cd frontend
npm install
npm run dev
```

## 3. 统一账号模型

系统仅保留一张账号表 `accounts`：

- `account_type=USER`：普通用户（房东/租客）
- `account_type=STAFF`：业务员
- `account_type=ADMIN`：管理员

公共字段：

- 登录：`username/password`
- 管理字段：`enabled/can_publish/can_rent`
- 资料字段：`real_name/phone/email/id_card/avatar/display_name`

## 4. 业务主流程

### 4.1 房源流程

1. 房东发布/编辑房源后进入 `PENDING_STAFF_REVIEW`
2. 业务员通过：`AVAILABLE`
3. 业务员驳回：`OFFLINE`

### 4.2 合同流程

1. 租客发起申请：`PENDING_LANDLORD_APPROVAL`
2. 房东同意：`PENDING_STAFF_SIGNING`
3. 业务员上传签约文件：`PENDING_ADMIN_APPROVAL`
4. 管理员通过：`ACTIVE`，房源变为 `RENTED`
5. 房东拒绝：`REJECTED`，房源恢复 `AVAILABLE`

### 4.3 终止流程

- 普通终止：对方确认 -> 业务员审核 -> 终止/驳回
- 强制终止：普通终止累计被拒达到 3 次后可发起，业务员+管理员联审

## 5. 消息中心规则

- 消息卡片按钮执行真实业务接口，不是单纯改消息状态。
- 动作成功后再调用 `/api/messages/{id}/status` 写回 `ACCEPT/REJECT`。
- 会话键使用 `contactType + contactId`（`USER/OPERATOR/SYSTEM`）避免串线。

## 6. 文件存储

- 合同签约文件：`uploads/contracts/{contractId}/signed/*`
- 强制终止证据：`uploads/contracts/{contractId}/termination/{requestId}/*`
- 房源图片：`uploads/houses/{houseId}/gallery/*`

数据库仅记录路径，不直接存二进制文件。

## 7. 常用构建命令

```bash
mvn -DskipTests clean compile
npm --prefix frontend run build
```
