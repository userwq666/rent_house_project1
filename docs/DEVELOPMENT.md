# 开发指南

## 快速启动
- 环境：JDK 21、Node 18+、MySQL 5.7，全部使用 UTF-8。
- 初始化数据库：创建库 `rent_house_db`，执行 `src/main/resources/schema-v3.sql`（含索引与字段调整）。默认不再自动插入演示数据，如需测试请自行导入。
- 启动后端：`mvn spring-boot:run` 或运行 `com.renthouse.RentHouseApplication`。
- 启动前端：`cd frontend && npm install && npm run dev`，默认 http://localhost:5173。

## 运行约定
- `spring.jpa.hibernate.ddl-auto=none`，启动时仅执行 schema 脚本，不强制跑 data 脚本。
- 认证使用 JWT，请在请求头携带 `Authorization: Bearer <token>`。
- 账号类型：`USER` 与 `ADMIN`；管理员接口仅 ADMIN 可用，普通接口需登录。

## 主要接口清单
- 认证：`POST /api/auth/register`（写入 accounts + users）、`POST /api/auth/login`、`GET/PUT /api/auth/profile`（读写 users）。
- 房源：公共 `GET /api/houses/available`、`GET /api/houses/search`、`GET /api/houses/{id}`；登录 `GET /api/houses/my`、`POST /api/houses`、`PUT /api/houses/{id}`、`DELETE /api/houses/{id}`、上下线 `/online|/offline`；管理员 `GET /api/houses/all`。
- 合同：`POST /api/contracts` 创建，`GET /api/contracts/my` 拉取个人合同，终止相关接口同控制器下。
- 消息与终止申请：`/api/messages` 获取/发送/已读；`/api/contracts/termination/...` 处理终止审批。

## 前端要点
- 图片上传：发布/编辑单张 ≤1MB，最多 4 张，支持 png/jpg/webp；使用文件选择器转 Base64，保存到 `houses.images`，用竖线 `|` 分隔，展示时按 `|` 拆分。
- 轮播与预览：列表卡片、详情页、编辑页使用 `el-image` 预览，点击可放大；多图时走 `el-carousel`。
- 登录流程：未登录访问详情会提示登录/注册；登录后按账号类型跳转（ADMIN 进后台，USER 回首页）。
- 合同页：按角色分栏，展示对方姓名/电话，操作按钮居中，支持联系对方、查看房源、申请终止。
- 后台：`/admin` 查看房源/合同/用户，删除房源调用真实接口，统计卡片基于实时数据。

## 数据与性能
- `houses.images` 为 `MEDIUMTEXT`，内容为 Base64 字符串列表（`|` 分隔）。
- 关键字段已加索引：accounts.username、houses.owner_id/status、contracts.landlord_id/tenant_id 等，避免高频查询退化。

## 常见排查
- 写入房源报 “images 太长”：确认 ≤4 张、每张 ≤1MB，且以 `|` 拼接。
- 注册后 users 为空：检查注册接口是否成功、JWT 是否携带、及数据库连接信息。
- Vue 构建报模板错误：检查标签闭合，避免手动输入的特殊字符。
