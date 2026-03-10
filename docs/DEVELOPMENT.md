# 开发指南

## 快速启动
- 环境：JDK 21、Node.js 18+、MySQL 5.7+。
- 数据库：默认库名 `easy_rent`。

### 1) 初始化数据库
```bash
mysql -u root -p < src/main/resources/init-database.sql
```

### 2) 启动后端
```bash
mvn spring-boot:run
```
- 启动时会执行 `schema-v3.sql`（`spring.sql.init.mode=always`）。
- `DataInitializer` 会自动补齐默认管理员：`admin / 123456`。

### 3) 启动前端
```bash
cd frontend
npm install
npm run dev
```
默认地址：`http://localhost:5173`。

## 账号体系
- 普通用户：`accounts + users`
- 管理员/业务员：`operator_accounts`（独立于普通用户表）

## 合同终止流程（v3）
### 普通终止
1. A/B 发起终止：合同状态 `TERMINATION_PENDING_COUNTERPARTY`。
2. 对方同意：合同状态 `TERMINATION_PENDING_STAFF_REVIEW`，通知业务员。
3. 对方拒绝：合同恢复 `ACTIVE`，并对发起人拒绝次数 `+1`。
4. 业务员审核：
   - 同意：合同 `TERMINATED`，房源 `AVAILABLE`。
   - 拒绝：合同回 `ACTIVE`。

### 强制终止
1. 触发条件：同一合同同一发起人普通终止被拒累计 `>=3`。
2. 发起时必须填写 `forceReason` 和 `evidenceUrls`。
3. 合同状态置为 `TERMINATION_FORCE_PENDING_JOINT_REVIEW`。
4. 业务员与管理员联合审核：
   - 业务员同意时必须填写后续方案。
   - 管理员同意时必须填写裁决说明。
5. 双方都同意后合同终止并释放房源。

## 主要接口补充
- `PUT /api/contracts/{id}/terminate`：发起普通/强制终止。
- `PUT /api/contracts/termination/{requestId}/counterparty-decision`：对方确认普通终止。
- `PUT /api/contracts/termination/{requestId}/decision`：业务员/管理员审核终止。
- `GET /api/contracts/staff/my`：业务员查看分配合同。
- `POST /api/contracts/{id}/signed-file`：业务员上传签约合同。
- `GET/POST/PUT /api/admin/operators/**`：管理员管理业务员账号。

## 常见问题
- 在 IDE 里执行 `SOURCE xxx.sql` 报错：IDE SQL 控制台通常不支持 `SOURCE`，请直接“打开并执行”对应 SQL 文件。
- 启动报 `Port 8080 was already in use`：关闭占用进程或改 `server.port`。
