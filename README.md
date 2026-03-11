# 租房管理系统

基于 `Spring Boot 3 + Vue 3` 的前后端分离项目，覆盖房源发布、租房申请、合同审批、合同终止、消息中心与后台管理流程。

## 技术栈

### 后端
- Java 21
- Spring Boot 3
- Spring Web / Spring Security / Spring Data JPA
- JWT
- MySQL

### 前端
- Vue 3 + Vite
- Vue Router
- Element Plus
- Axios

## 目录结构

```text
src/main/java/com/renthouse      后端核心代码
src/main/resources               配置与 SQL 脚本
frontend/                        前端项目
docs/                            文档
pom.xml                          Maven 配置
```

## 核心重构说明（rank2）

- 统一账号模型：仅保留 `accounts`，通过 `account_type=USER/STAFF/ADMIN` 区分身份。
- 删除旧表：`users`、`operator_accounts` 已从 schema 中移除。
- 消息会话稳定化：会话分组继续使用 `contactType + contactId`，避免串线。
- 消息动作执行业务：消息卡片操作先调用业务接口，再回写消息状态 `ACCEPT/REJECT`。
- 文件存储改造：
  - 合同签约文件：`uploads/contracts/{contractId}/signed/*`
  - 强制终止证据：`uploads/contracts/{contractId}/termination/{requestId}/*`
  - 房源图片：`uploads/houses/{houseId}/gallery/*`
  - 数据库保存访问路径（如 `signed_contract_url`、`evidence_urls`、`houses.images`）。

## 快速启动

### 1) 初始化数据库

```bash
mysql -u root -p < src/main/resources/init-database.sql
```

### 2) 配置后端数据库连接

编辑 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/easy_rent?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=你的数据库密码
```

### 3) 启动后端

```bash
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 4) 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 默认管理员账号

- 用户名：`admin`
- 密码：`123456`

## 关键接口（节选）

### 认证
- `POST /api/auth/register`（支持 `accountType`，创建 `STAFF` 需管理员登录态）
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/auth/contact/{username}`

### 合同
- `POST /api/contracts`
- `PUT /api/contracts/{id}/landlord/approve`
- `PUT /api/contracts/{id}/landlord/reject`
- `POST /api/contracts/{id}/signed-file`
- `PUT /api/contracts/{id}/admin/approve`
- `PUT /api/contracts/{id}/admin/reject`
- `PUT /api/contracts/termination/{requestId}/counterparty-decision`
- `PUT /api/contracts/termination/{requestId}/decision`

### 房源审核
- `PUT /api/houses/{id}/staff/approve`
- `PUT /api/houses/{id}/staff/reject`

### 房源搜索
- `GET /api/houses/search`
  - 支持参数：`district`、`minPrice`、`maxPrice`、`houseType`、`minArea`、`maxArea`、`sortBy`
  - `keyword` 规则：
    - 普通关键词：匹配 `title/address/district/description/facilities`
    - 精确ID：`#123` 或 `id:123`（直接按房源ID匹配）

### 消息中心
- `GET /api/messages/contacts`
- `GET /api/messages/chat/{contactId}?contactType=USER|STAFF|ADMIN|SYSTEM`
- `POST /api/messages`
- `POST /api/messages/{id}/status`

## 构建验证

```bash
mvn -DskipTests clean compile
npm --prefix frontend run build
```
