# 租房管理系统开发文档

## 1. 项目概述

这是一个基于Spring Boot + Vue的租房管理系统，支持房东发布房源、租客租赁房屋、合同管理、消息通知等完整功能。

### 1.1 核心功能模块

1. **用户与账户管理**：账户体系分为管理员(ADMIN)和普通用户(USER)两种类型，具有不同的操作权限。
2. **房源管理**：房东可以发布房源信息，包括标题、地址、户型、面积、价格等，支持房源搜索和状态管理。
3. **合同管理**：支持租客与房东签署租房合同，提供正常和强制终止合同流程。
4. **消息系统**：系统内建消息机制，用于通知用户各种操作和事件。
5. **管理员功能**：提供用户管理、房源管理、合同管理等后台管理功能。

## 2. 技术栈

- 后端框架：Spring Boot 3.x + Java
- 前端框架：Vue.js
- 核心依赖：
  - Spring Web
  - Spring Data JPA
  - Lombok
  - Swagger (SpringDoc)
  - Spring Security
- 数据库：MySQL

## 3. 系统架构设计

### 3.1 分层架构

```
┌─────────────────┐
│   Controller    │ ← 处理HTTP请求与响应，定义API接口
├─────────────────┤
│    Service      │ ← 实现业务逻辑，事务管理，数据校验
├─────────────────┤
│   Repository    │ ← 数据持久化操作，数据库查询逻辑
├─────────────────┤
│    Entity       │ ← 数据库表结构映射对象
└─────────────────┘
```

### 3.2 包结构说明

```
com.renthouse
├── config              # 配置类
├── controller          # 控制器层
├── domain              # 实体类
├── dto                 # 数据传输对象
├── enums               # 枚举类
├── repository          # 数据访问层
├── security            # 安全认证相关
├── service             # 业务逻辑层
├── util                # 工具类
└── RentHouseApplication.java # 启动类
```

## 4. 核心实体设计

### 4.1 用户体系

#### Account（账户）
- 登录凭证和权限管理
- 字段：username(用户名)、password(密码)、accountType(账户类型)、enabled(是否启用)、canPublish(能否发布)、canRent(能否租赁)

#### User（用户）
- 用户详细信息
- 字段：realName(真实姓名)、phone(手机号)、email(邮箱)、idCard(身份证号)、avatar(头像)

### 41 房源管理

#### House（房源）
- 房源信息实体
- 字段：title(标题)、address(地址)、district(区域)、houseType(户型)、area(面积)、rentPrice(租金)、deposit(押金)、status(状态)等

#### HouseStatus（房源状态枚举）
- AVAILABLE(可租)
- RENTED(已出租)
- OFFLINE(已下架)

### 4.3 合同管理

#### Contract（合同）
- 租房合同实体
- 字段：house(房源)、landlord(房东)、tenant(租客)、rentPrice(租金)、deposit(押金)、startDate(开始日期)、endDate(结束日期)、status(状态)等

#### ContractStatus（合同状态枚举）
- ACTIVE(进行中)
- EXPIRED(已到期)
- TERMINATED(已终止)
- TERMINATION_PENDING(待终止确认)

#### TerminationRequest（终止申请）
- 合同终止申请记录
- 字段：contract(合同)、requester(申请人)、responder(确认人)、status(状态)、reason(原因)等

#### TerminationStatus（终止状态枚举）
- PENDING(待处理)
- APPROVED(已同意)
- REJECTED(已拒绝)
- FORCE_TERMINATED(强制终止)

### 4.4 消息系统

#### Message（消息）
- 系统消息/对话记录
- 字段：sender(发送者)、receiver(接收者)、title(标题)、content(内容)、type(类型)、status(状态)等

#### MessageType（消息类型枚举）
- USER_CHAT(用户聊天)
- TERMINATION_REQUEST(终止请求)
- TERMINATION_RESPONSE(终止响应)
- FORCE_TERMINATION_NOTICE(强制终止通知)
- ADMIN_NOTIFICATION(管理员通知)

#### MessageStatus（消息状态枚举）
- UNREAD(未读)
- READ(已读)

## 5. API接口设计

### 5.1 认证相关接口

- POST /api/auth/register - 用户注册
- POST /api/auth/login - 用户登录

### 5.2 房源管理接口

#### 公开接口
- GET /api/houses/available - 获取所有可租房源
- GET /api/houses/search - 搜索房源
- GET /api/houses/{id} - 根据ID获取房源详情

#### 需登录接口
- GET /api/houses/my - 获取我发布的房源
- POST /api/houses - 发布房源
- PUT /api/houses/{id} - 更新房源
- PUT /api/houses/{id}/offline - 下架房源
- PUT /api/houses/{id}/online - 上架房源
- DELETE /api/houses/{id} - 删除房源

#### 管理员接口
- GET /api/houses/all - 管理员获取全部房源

### 5.3 合同管理接口

- POST /api/contracts - 创建合同（签约）
- GET /api/contracts/my - 获取我的合同（作为房主或租客）
- GET /api/contracts/as-landlord - 获取作为房主的合同
- GET /api/contracts/as-tenant - 获取作为租客的合同
- GET /api/contracts/{id} - 根据ID获取合同详情
- PUT /api/contracts/{id}/terminate - 终止合同（退租）
- PUT /api/contracts/termination/{requestId}/decision - 对终止申请做决策
- PUT /api/contracts/{id}/admin/terminate - 管理员直接终止合同
- GET /api/contracts/all - 获取所有合同（仅管理员）

### 5.4 消息管理接口

- GET /api/messages - 获取我的消息
- POST /api/messages/{id}/read - 标记消息为已读
- POST /api/messages - 发送消息
- GET /api/messages/admin - 获取管理员消息

### 5.5 用户管理接口（管理员）

- GET /api/admin/users - 获取所有用户
- PUT /api/admin/users/{userId}/restrictions - 更新用户限制
- PUT /api/admin/users/{userId}/status - 更新账户状态

## 6. 数据库设计

### 6.1 表结构

参见 [schema-v3.sql](../src/main/resources/schema-v3.sql)

### 6.2 主要表关系

```
accounts (1:1) users (1:N) houses
                  (1:N) landlord_contracts
                  (1:N) tenant_contracts

houses (1:N) contracts (N:1) users(landlord)
             (N:1) users(tenant)

contracts (1:N) termination_requests (N:1) users(requester)
                                    (N:1) users(responder)

users(sender) (N:N) messages (N:1) users(receiver)
```

## 7. 权限控制

### 7.1 角色权限

- **普通用户**：可以发布房源、租赁房屋、管理自己的合同和消息
- **管理员**：拥有系统所有功能的操作权限

### 7.2 操作权限

1. 只有房主或管理员可以编辑/删除房源
2. 只能操作自己相关的合同
3. 管理员可以查看和管理所有用户、房源和合同
4. 用户只能给自己发消息或者给管理员发消息

## 8. 业务流程

### 8.1 房源发布流程

```
用户登录 → 进入发布页面 → 填写房源信息 → 提交 → 系统验证 → 保存房源 → 返回成功信息
```

### 8.2 租房签约流程

```
租客查看房源 → 确认租赁 → 填写合同信息 → 提交 → 系统验证 → 生成合同 → 更新房源状态为已出租
```

### 8.3 合同终止流程

```
用户发起终止申请 → 填写终止原因 → 提交 → 系统验证 → 创建终止申请 → 通知对方用户 → 对方确认/拒绝 → 更新合同状态
```

### 8.4 强制终止流程

```
用户选择强制终止 → 填写强制终止原因 → 提交 → 系统验证 → 直接终止合同 → 通知对方用户和管理员 → 更新用户权限
```

## 9. 安全设计

### 9.1 认证机制

- 使用JWT Token进行身份认证
- 密码使用BCrypt加密存储

### 9.2 权限验证

- 基于角色的访问控制(RBAC)
- 操作前进行权限检查

### 9.3 输入验证

- 使用JSR-303注解进行数据校验
- 防止SQL注入等安全风险

## 10. 部署说明

### 10.1 环境要求

- JDK 17+
- MySQL 5.7+
- Maven 3.6+

### 10.2 配置文件

配置文件位于 [application.properties](../src/main/resources/application.properties)

关键配置项：
```properties
# 数据库连接配置
spring.datasource.url=jdbc:mysql://localhost:3306/rent_house_db
spring.datasource.username=root
spring.datasource.password=123456

# JWT密钥
jwt.secret=mySecretKey
```

### 10.3 初始化脚本

系统启动时会自动执行以下脚本：
- [schema-v3.sql](../src/main/resources/schema-v3.sql) - 数据库表结构
- [init-data-placeholder.sql](../src/main/resources/init-data-placeholder.sql) - 初始数据

## 11. 开发规范

### 11.1 代码规范

遵循SOLID、DRY、KISS、YAGNI原则，参考阿里巴巴Java开发手册。

### 11.2 命名规范

- 类名：大驼峰命名法（如UserService）
- 方法/变量名：小驼峰命名法（如getUserInfo）
- 常量：全大写加下划线（如MAX_RETRY_COUNT）

### 11.3 注释规范

- 类和公共方法必须有Javadoc注释
- 业务逻辑复杂处需要添加行内注释
- 待办事项使用TODO标记
- 潜在问题使用FIXME标记

### 11.4 异常处理

- 统一异常处理机制
- 使用全局异常处理器捕获和处理异常
- 返回统一的错误响应格式

### 11.5 日志规范

- 使用SLF4J记录日志
- 核心操作记录INFO级别日志
- 异常情况记录ERROR级别日志
