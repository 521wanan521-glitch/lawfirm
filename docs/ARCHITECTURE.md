# 系统架构说明

## 总体架构

```
┌─────────────────────────────────────────────┐
│               浏览器（员工电脑/手机）           │
└─────────────────────┬───────────────────────┘
                      │ HTTPS (443)
┌─────────────────────▼───────────────────────┐
│             Nginx（前端容器）                 │
│  静态资源(SPA)  /  /api 反向代理到 backend    │
└───────┬──────────────────────────┬──────────┘
        │ /api                     │
┌───────▼──────────┐      ┌────────▼─────────┐
│  backend 容器     │      │  前端静态文件      │
│ Spring Boot 3    │      │  (Vue3 构建产物)   │
│ 端口 8080        │      └──────────────────┘
└───────┬──────────┘
        │ JDBC
┌───────▼──────────┐      ┌──────────────────┐
│  PostgreSQL 16    │      │  上传文件 Volume   │
│  (db 容器)        │      │  (uploads)        │
└──────────────────┘      └──────────────────┘
```

- 单台 ECS 运行三个 Docker 容器：`db`、`backend`、`frontend`，由 docker-compose 编排。
- 前端容器内 Nginx 同时承担静态资源托管与 `/api` 反向代理，前后端同源，天然规避 CORS。

## 后端设计

- **分层**：Controller → Service → Repository（Spring Data JPA），DTO 使用 Java Record 精简。
- **认证**：JWT（jjwt 0.12），无状态；`JwtAuthFilter` 从 `Authorization: Bearer` 或 `?token=`（文件下载）解析令牌；BCrypt 加密密码。
- **权限**：基于角色的 `@PreAuthorize` + 服务内二次校验；`CurrentUser` 工具类获取当前登录人。
- **统一响应**：`ApiResponse{code, message, data}`，`GlobalExceptionHandler` 统一处理业务异常与参数校验。
- **数据模型**：Hibernate `ddl-auto: update` 自动建表，首次启动由 `DataSeeder` 初始化默认账号与演示数据。
- **文件存储**：`app.upload-dir` 指定目录，文件名 UUID 化存储，`doc_version` 表维护多版本。

### 模块与主要实体

| 模块 | 包 | 主要实体 |
| --- | --- | --- |
| 认证/用户 | auth / user | sys_user |
| 客户 CRM | client | crm_client / crm_contact / crm_interaction |
| 案件 | cases | case_case / case_progress / case_co_lawyer |
| 计费 | billing | bill_time_entry / bill_invoice / bill_invoice_time |
| 文档 | document | doc_document / doc_version / doc_folder |
| 日程 | calendar | cal_event / cal_participant |
| 审批 | approval | appr_template / appr_instance |
| 知识库 | knowledge | know_article |

## 前端设计

- Vue 3 Composition API + `<script setup>`，Element Plus 组件库（中文 locale），Pinia 状态管理。
- 路由懒加载 + 全局登录守卫；Axios 拦截器统一携带 JWT、处理 401 跳转登录。
- 枚举展示（状态/类型标签）统一收敛在 `utils/dict.js`，格式化工具在 `utils/format.js`。
- 统计图表使用 ECharts。

## 环境与配置

- **dev 环境**：H2 文件数据库（MODE=PostgreSQL），本地 `mvn spring-boot:run` 即开即用；`application.yml` 中 `spring.profiles.active` 默认 `dev`。
- **prod 环境**：PostgreSQL，通过环境变量注入（DB_HOST/DB_NAME/DB_USER/DB_PASSWORD/APP_JWT_SECRET/APP_UPLOAD_DIR）。
- JWT 密钥、数据库密码等敏感配置一律通过环境变量注入，不写入代码。

## 扩展建议

- **多级审批**：将 `appr_instance.approverId` 单级审批扩展为审批链（approval_flow 表 + 多任务节点）。
- **消息通知**：接入钉钉/企业微信/邮件，审批、开庭提醒自动推送。
- **电子签章**：对接法大大/e签宝 API，与文档中心集成。
- **更细粒度权限**：案件级可见性（主办/协办/部门）、文档访问控制列表。
- **对象存储**：文件量增大后可将存储层替换为阿里云 OSS。
- **监控告警**：接入 Spring Boot Actuator + Prometheus，数据库慢查询日志。
