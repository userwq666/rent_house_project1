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

## 核心功能

- 用户注册登录、个人资料维护
- 房东发布/编辑/上下架房源
- 租客发起租房申请（合同创建）
- 合同多阶段流转：房东审批 -> 业务员签约 -> 管理员审核
- 普通终止与强制终止（业务员/管理员联审）
- 消息中心支持“执行业务动作”并回写消息状态
- 联系人支持 USER / OPERATOR 统一检索与会话

## 快速启动

### 1. 初始化数据库

```bash
mysql -u root -p < src/main/resources/init-database.sql
```

### 2. 配置后端数据库连接

编辑 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/easy_rent?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=你的数据库密码
```

### 3. 启动后端

```bash
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 默认管理员账号

- 用户名：`admin`
- 密码：`123456`

建议首次登录后立即修改。

## 关键接口（节选）

### 认证
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/auth/contact/{username}` 统一联系人查询（USER/OPERATOR）

### 合同
- `POST /api/contracts` 创建合同（租房申请）
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

### 消息中心
- `GET /api/messages/contacts`
- `GET /api/messages/chat/{contactId}?contactType=USER|OPERATOR|SYSTEM`
- `POST /api/messages`
- `POST /api/messages/{id}/status`

## 构建验证

```bash
mvn -DskipTests compile
npm --prefix frontend run build
```

## 说明

- `schema.sql` 与 `schema-v3.sql` 已包含消息表 `related_house_id`、操作员收发字段等结构。
- 消息中心动作遵循“先执行业务接口，再回写消息状态（ACCEPT/REJECT）”。
