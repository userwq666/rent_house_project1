# 租房管理系统

一个基于 **Spring Boot + Vue 3** 的前后端分离租房管理系统，支持用户注册登录、房源发布与浏览、合同管理、消息通知、业务员后台和管理员后台。

## 项目简介

本项目面向租房场景，提供完整的业务闭环：

- 普通用户注册、登录、个人资料管理
- 房东发布、编辑、上下架房源
- 租客浏览房源、申请签约
- 房东与管理员审批合同
- 合同终止申请与处理
- 用户消息中心与系统通知
- 管理员统一管理用户、房源、合同

## 技术栈

### 后端
- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- MySQL

### 前端
- Vue 3
- Vite
- Vue Router
- Element Plus
- Axios

## 项目结构

```bash
rent_house_project1
├── src/main/java/com/renthouse    # 后端核心代码
├── src/main/resources             # 配置文件与数据库脚本
├── frontend                       # 前端项目
├── docs                           # 项目文档
├── pom.xml                        # Maven 配置
└── .gitignore
```

## 核心功能

### 1. 用户与认证
- 用户注册与登录
- JWT 身份认证
- 个人资料查看与修改
- 普通用户 / 管理员角色区分

### 2. 房源管理
- 发布房源
- 编辑房源
- 上下架房源
- 房源搜索与筛选
- 房源详情查看
- 多图片展示

### 3. 合同管理
- 创建租房合同
- 房东审批合同
- 管理员审批合同
- 查看我的合同
- 终止合同申请
- 强制终止处理

### 4. 消息中心
- 用户聊天消息
- 系统通知
- 审批提醒
- 未读消息统计

### 5. 管理员后台
- 用户管理
- 房源管理
- 合同管理
- 权限控制
- 业务员账号管理（新增/停用）

## 运行环境

- JDK 21
- Node.js 18+
- MySQL 5.7 及以上
- Maven 3.6 及以上

## 快速启动

### 1. 克隆项目
```bash
git clone https://github.com/userwq666/rent_house_project1.git
cd rent_house_project1
```

### 2. 配置数据库
修改后端配置文件：

`src/main/resources/application.properties`

根据本地 MySQL 环境修改：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/easy_rent?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=你的数据库密码
```

首次初始化推荐先执行：

```bash
mysql -u root -p < src/main/resources/init-database.sql
```

然后启动后端，应用会自动执行 `schema-v3.sql` 建表，并初始化默认管理员。

### 3. 启动后端
```bash
mvn spring-boot:run
```

默认启动地址：

```bash
http://localhost:8080
```

### 4. 启动前端
```bash
cd frontend
npm install
npm run dev
```

默认启动地址：

```bash
http://localhost:5173
```

## 默认管理员账号

系统首次启动时会自动初始化管理员账号：

- 用户名：`admin`
- 密码：`123456`

建议首次登录后立即修改。

## 主要接口说明

### 认证接口
- `POST /api/auth/register` 用户注册
- `POST /api/auth/login` 用户登录
- `GET /api/auth/profile` 获取个人资料
- `PUT /api/auth/profile` 更新个人资料

### 房源接口
- `GET /api/houses/available` 获取可租房源
- `GET /api/houses/search` 搜索房源
- `GET /api/houses/{id}` 获取房源详情
- `POST /api/houses` 发布房源
- `PUT /api/houses/{id}` 更新房源
- `DELETE /api/houses/{id}` 删除房源

### 合同接口
- `POST /api/contracts` 创建合同
- `GET /api/contracts/my` 获取我的合同
- `PUT /api/contracts/{id}/terminate` 发起终止（普通/强制）
- `PUT /api/contracts/termination/{requestId}/counterparty-decision` 对方同意/拒绝普通终止
- `PUT /api/contracts/termination/{requestId}/decision` 业务员/管理员终止审核（强制终止为联合审核）
- `PUT /api/contracts/{id}/landlord/approve` 房东审批
- `PUT /api/contracts/{id}/admin/approve` 管理员审批

### 消息接口
- `GET /api/messages` 获取消息
- `GET /api/messages/contacts` 获取联系人
- `GET /api/messages/chat/{contactId}` 获取聊天记录
- `POST /api/messages` 发送消息

## 数据库设计

主要表包括：

- `accounts` 账号表
- `users` 用户信息表
- `operator_accounts` 管理员/业务员账号表
- `houses` 房源表
- `contracts` 合同表
- `termination_requests` 合同终止申请表
- `messages` 消息表

数据库脚本位置：

```bash
src/main/resources/schema-v3.sql
```

## 开发说明

项目文档位于：

- `docs/DEVELOPMENT.md`
- `docs/development-guide.md`
- `docs/ENCODING.md`（编码规范与乱码排查）

## 后续优化方向

- 合同 PDF 导出
- 电子签章
- 更细粒度的权限控制
- 文件上传改为对象存储
- Docker 部署支持

## License

本项目仅用于学习与毕业设计实践。
