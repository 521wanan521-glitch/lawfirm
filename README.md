# 律所数字化办公系统（LawFirm）

面向 20-100 人律师事务所的一体化数字化办公平台，覆盖律所日常运营全流程。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 案件管理 | 立案、承办、进程跟踪、状态流转（待立案→办理中→结案→归档）、自动案号 |
| 客户管理 CRM | 客户档案、联系人、跟进记录、客户分级 |
| 计时计费 | 工时记录、审核流转、账单生成（草稿→开票→收款→作废） |
| 文档中心 | 多级目录、文件上传/下载、多版本管理、按案件/客户关联 |
| 日程安排 | 月/周/日视图、开庭/会议/任务/提醒、参与人 |
| 审批流程 | 用章、请假、报销、立案审批等模板、发起→审批→归档 |
| 知识库 | 办案经验、法规、文书模板沉淀，发布/草稿机制 |
| 统计报表 | 案件类型分布、月度趋势、律师工时排行、经营数据看板 |
| 成员管理 | 角色权限（管理员/合伙人/律师/助理/行政）、账号启停、密码重置 |

## 技术栈

- **后端**：Java 17 · Spring Boot 3.2 · Spring Security + JWT · Spring Data JPA · PostgreSQL 16（开发环境内置 H2）
- **前端**：Vue 3 · Vite 5 · Element Plus · Pinia · Vue Router · ECharts
- **部署**：Docker Compose + Nginx（单机三容器：db / backend / frontend）

## 目录结构

```
lawfirm/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── deploy/           # docker-compose 与 .env 示例
└── docs/             # 部署与使用文档
```

## 快速开始（本地开发）

后端（默认 H2 内存/文件数据库，无需安装数据库）：

```bash
cd backend
mvn spring-boot:run
# API 文档: http://localhost:8080/api/swagger-ui.html
```

前端：

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173 （已配置 /api 代理到 8080）
```

首次启动自动创建演示账号：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | 系统管理员 |
| partner | partner123 | 合伙人 |
| lawyer1 / lawyer2 | lawyer123 | 执业律师 |
| paralegal | paralegal123 | 律师助理 |
| staff | staff123 | 行政 |

> ⚠️ 正式上线后请立即修改所有默认密码。

## 生产部署（Docker）

详见 [docs/DEPLOY_ALIYUN.md](docs/DEPLOY_ALIYUN.md)——阿里云 ECS 完整部署指南。

```bash
cd deploy
cp .env.example .env   # 修改数据库密码与 JWT 密钥
docker compose up -d --build
```

## 文档

- [阿里云部署指南](docs/DEPLOY_ALIYUN.md)
- [用户使用手册](docs/USER_GUIDE.md)
- [系统架构说明](docs/ARCHITECTURE.md)
