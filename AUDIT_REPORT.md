# lawfirm 系统并行代码审计报告

> 审计方式：DeepSeek Harness Workflow，10 个并行审计子代理（后端 7 个模块 + 前端 2 个 + 部署 1 个）+ 1 个汇总代理。
> 覆盖：Spring Boot 3 后端全模块、Vue3 + Element Plus 前端、部署与构建配置。

## 总体结论

lawfirm 系统功能模块完整、技术选型较规范（BCrypt、JWT、BigDecimal、JPA+Specification），但**安全边界存在系统性缺失**：授权控制未贯穿全模块，越权（IDOR）几乎覆盖客户、案件、文档、账单、仪表盘等所有核心数据域；认证本身更依赖硬编码默认 JWT 密钥与公开种子口令，可被直接伪造管理员令牌或登录接管。

最严重的三类问题：

1. **硬编码默认 JWT 密钥**，可伪造任意用户（含 ADMIN）令牌；
2. **H2 控制台默认 dev 空密码无条件放行**，未认证即可暴露整库；
3. **文档上传扩展名未过滤导致路径穿越**，可任意文件写入与越界读取。

**整体健康度偏低：在鉴权加固完成前不宜对外暴露生产环境。**

## 统计

| 严重度 | 数量 |
| --- | --- |
| Critical | 3 |
| High | 7 |
| Medium | 19 |
| Low | 23 |
| Info | 6 |
| **合计** | **58** |

---

## Critical（3）

### 1. 硬编码默认 JWT 密钥，可伪造任意用户（含 ADMIN）令牌
- **位置**：`backend/src/main/resources/application.yml#app.jwt.secret`（JwtService 以该值签名/校验）
- **说明**：配置 `secret: ${APP_JWT_SECRET:LawFirmSystemDefaultSecretKeyChangeMeInProduction2024!}`，未注入环境变量时静默回退到仓库公开的固定密钥。token 仅含 userId/username/role，任何拿到源码者可签发 `subject=任意 userId、role=ADMIN` 的合法 token，完全绕过认证与授权。
- **建议**：移除默认回退值改为 `${APP_JWT_SECRET}` 无默认值，并在启动时强制校验密钥存在且长度 ≥32，缺失或过短 fail-fast。

### 2. H2 控制台被无条件 permitAll，默认 dev 环境以空密码暴露整库
- **位置**：`backend/src/main/java/com/lawfirm/security/SecurityConfig.java#filterChain`
- **说明**：`/h2-console/**` 无条件 `permitAll`（不区分环境）；默认 dev 下 H2 console `enabled=true`、数据源 `jdbc:h2:file:./data/lawfirm-dev`、用户 `sa`、空密码并开 `AUTO_SERVER`。任何未登录者可读取/篡改全部数据（含密码哈希）。
- **建议**：删除 `/h2-console/**` 的 permitAll；H2 console 仅 dev 通过 profile 条件启用、绑定 localhost、设强密码，生产严禁开启。

### 3. 文档上传路径穿越：扩展名未校验可任意文件写入/越界读取
- **位置**：`backend/src/main/java/com/lawfirm/document/DocumentService.java#storeFile`（download 复用 resolvePath）
- **说明**：`storeFile` 直接取原始文件名最后一个 `.` 之后的子串作为扩展名，未做白名单/字符校验；构造 `x./../../path` 类文件名可令 storedName 拼入 `../`，经 `Paths.get(uploadDir).resolve(storedName).normalize()` 逃出上传目录写任意文件；`download` 同样不校验归一化后仍在上传目录内，可越界读取任意文件。
- **建议**：扩展名严格白名单并拒绝含 `/`、`\`、`..` 或控制字符的值；归一化后校验 `path.startsWith(uploadDirNormalized)`；文件名/扩展名尽量服务端生成。

---

## High（7）

### 1. DataSeeder 内置公开默认口令并明文记日志
- **位置**：`backend/src/main/java/com/lawfirm/config/DataSeeder.java#run`
- **说明**：首次启动自动创建 `admin/admin123`、`partner/partner123`、`lawyer1/lawyer123`、`paralegal/paralegal123`、`staff/staff123` 等固定口令账号并 `log.info` 明文打印；`deploy/deploy.sh` 与前端 `Login.vue` 也明文展示演示账号。
- **建议**：移除硬编码口令，改启动随机一次性口令或强制首次登录改密；日志/脚本/登录页严禁输出口令。

### 2. 案件模块完全缺失权限与归属校验（IDOR，全端点）
- **位置**：`backend/src/main/java/com/lawfirm/cases/CaseController.java#detail`（及 page/create/update/updateStatus/delete/progress/addProgress）
- **说明**：全端点无 `@PreAuthorize`，Service 无角色或主办/协办归属校验；任何登录用户可枚举并读取全所案件，并可修改、删除、变更状态、伪造进程记录。
- **建议**：写接口加 `@PreAuthorize`；Service 层校验 `leadLawyerId==CurrentUser.id()` 或 coLawyerIds 包含当前用户（管理员除外）。

### 3. 客户模块无权限/归属校验，任意登录用户可读写删全量客户 PII
- **位置**：`backend/src/main/java/com/lawfirm/client/ClientController.java#page`（detail/update/delete/contacts/interactions）
- **说明**：所有 `/clients` 接口无 `@PreAuthorize`，无角色或 ownerId 校验；任意登录用户可越权查看/修改/删除任意客户及联系人、跟进记录，`page()` 把 idNumber（证件号）、phone、email 全量返回。
- **建议**：各方法增加 ownerId/管理员校验或 `@PreAuthorize`；列表按当前用户过滤或对敏感字段脱敏。

### 4. 文档读取/下载越权（IDOR），任意登录用户可读任意机密文件
- **位置**：`backend/src/main/java/com/lawfirm/document/DocumentService.java#download`（及 detail/versions/page）
- **说明**：仅 `delete` 校验了 uploadedBy/管理员，`detail/versions/download/page` 均无校验；任意登录用户遍历自增 id 即可下载任意案件证据、合同等机密文件。
- **建议**：为 detail/versions/download/page 增加授权，统一封装 `checkAccess(doc)` 复用。

### 5. 账单状态变更接口缺权限校验，任意用户可开票/收款/作废
- **位置**：`backend/src/main/java/com/lawfirm/billing/BillingService.java#updateInvoiceStatus`
- **说明**：未调用 `requireManager()` 也无属主/角色校验；任何登录用户可对任意账单执行 DRAFT→ISSUED、ISSUED→PAID 或 VOID（VOID 还会释放已开票工时）。
- **建议**：`updateInvoiceStatus` 开头增加 `requireManager()` 或 `@PreAuthorize`，VOID 加严格前置状态校验。

### 6. 账单列表/详情无授权，越权读取全所财务数据
- **位置**：`backend/src/main/java/com/lawfirm/billing/BillingService.java#pageInvoices`（及 invoiceDetail）
- **说明**：无角色或属主过滤，任何已认证用户可列出全部账单并按 id 读任意账单明细（对比 pageTimeEntries 已对非管理员过滤）。
- **建议**：非 ADMIN/PARTNER 仅返回本人相关账单，或增加 `requireManager()`/`@PreAuthorize`。

### 7. 仪表盘接口缺角色鉴权，任意登录用户可查看全所经营数据
- **位置**：`backend/src/main/java/com/lawfirm/dashboard/DashboardController.java#summary`（及 stats）
- **说明**：summary/stats 无 `@PreAuthorize`，任意 LAWYER/PARALEGAL/STAFF 可访问案件/客户总数、营收、律师工时排行、最近案件等经营数据。
- **建议**：`/dashboard/**` 限制 ADMIN/PARTNER，非管理角色仅返回本人相关聚合数据。

---

## Medium（19）

| # | 问题 | 位置 |
| --- | --- | --- |
| 1 | 全局异常处理器回显内部异常信息（SQL/表名/路径泄露） | `common/GlobalExceptionHandler.java#handleOther`、`document/DocumentService.java#storeFile` |
| 2 | JWT 通过 URL 查询参数传递（后端支持 + 前端下载使用） | `security/JwtAuthFilter.java#resolveToken`、`frontend/src/api/document.js#downloadUrl` |
| 3 | JWT 与完整用户对象明文存 localStorage，易被 XSS 窃取 | `frontend/src/store/user.js#setLogin`、`api/request.js` |
| 4 | 前端路由守卫只校验 token、不校验角色，adminOnly 仅隐藏菜单 | `frontend/src/router/index.js#beforeEach`、`layout/Layout.vue` |
| 5 | `/users/options` 未限角色，任意登录用户枚举全员 PII | `user/UserController.java#options` |
| 6 | 修改/重置密码后旧 JWT 不失效（默认 12h 仍有效） | `auth/AuthService.java#changePassword`、`user/UserService.java#resetPassword` |
| 7 | 分页参数 page/size 全系统无边界校验（可近全表加载/500） | User/Client/Case/Billing/Document/Approval/Knowledge Service 各 `#page` |
| 8 | 分页/列表系统性 N+1 查询（1+2N~1+3N 条 SQL） | Case/Client/Document/Billing/Calendar/Approval/Dashboard 各 `#toView` |
| 9 | 案号/账单号以 count+1 生成，并发重号与唯一键冲突 | `cases/CaseService.java#generateCaseNo`、`billing/BillingService.java#generateInvoiceNo` |
| 10 | 删除客户/案件未处理关联数据，产生孤儿记录且缺外键约束 | `client/ClientService.java#delete`、`cases/CaseService.java#delete` |
| 11 | 创建/修改工时不校验案件参与权限，任意用户可为任意案件记工时 | `billing/BillingService.java#createTimeEntry` |
| 12 | rate 可空导致 amount 为 null，开票总额被静默少计 | `billing/BillingService.java#apply` |
| 13 | addVersion 版本号非原子递增，并发产生重复版本 | `document/DocumentService.java#addVersion` |
| 14 | 案件状态流转无校验，离开 CLOSED 后 closeDate/result 不清理 | `cases/CaseService.java#updateStatus` |
| 15 | 生产环境开放免鉴权 Swagger/OpenAPI 文档 | `application.yml#springdoc`、`SecurityConfig#permitAll` |
| 16 | 数据库口令存在可猜测的弱默认回退 | `deploy/docker-compose.yml#db.environment`、`application.yml#prod` |
| 17 | 生产环境使用 ddl-auto:update 自动改表 | `application.yml#spring.jpa.hibernate.ddl-auto(prod)` |
| 18 | 仅 HTTP 明文传输，登录凭据可被窃听 | `deploy/docker-compose.yml#frontend.ports` |
| 19 | deploy.sh 直接管道执行远程安装脚本（供应链投毒风险） | `deploy/deploy.sh#安装 Docker` |

---

## Low（23，精简表）

| 问题 | 位置 |
| --- | --- |
| 登录响应区分账号已停用，可枚举账号状态 | `auth/AuthService.java#login` |
| 登录接口无速率限制/暴力破解防护 | `auth/AuthController.java#login` |
| 创建用户用户名查重存在 check-then-act 竞态 | `user/UserService.java#create` |
| 管理员可停用/删除自身或最后一名管理员 | `user/UserController.java#setEnabled` |
| CORS 对任意来源放开且允许任意请求头 | `security/SecurityConfig.java#corsConfigurationSource` |
| primaryContact 唯一性未约束 | `client/ClientService.java#apply` |
| 案件进程日期 progressDate 可任意指定 | `cases/CaseService.java#addProgress` |
| 工时 submit 为空操作、reject 无独立状态 | `billing/BillingService.java#submit` |
| 工时金额乘法未对齐列精度，可能舍入误差 | `billing/BillingService.java#apply` |
| 删除目录/审批模板时全表加载做存在性判断 | `document/DocumentService.java#deleteFolder`、`approval/ApprovalService.java#deleteTemplate` |
| 多模块写入关联 ID 未校验存在性，产生孤儿引用 | `document/DocumentService.java#upload`、`calendar/CalendarService.java#apply` |
| contentType 未校验，畸形值 500 且可被反射控制响应类型 | `document/DocumentController.java#download` |
| 删除文档先删物理文件后删库，回滚后孤儿元数据 | `document/DocumentService.java#delete` |
| 目录操作缺属主/角色校验 | `document/DocumentService.java#createFolder/deleteFolder` |
| 可对已停用审批模板发起审批 | `approval/ApprovalService.java#create` |
| 审批决定/撤销存在并发竞态（无乐观锁） | `approval/ApprovalService.java#decide` |
| 文章浏览量并发丢失更新 | `knowledge/KnowledgeService.java#detail` |
| 日程列表向所有登录用户返回全员日程 | `calendar/CalendarService.java#events` |
| store 初始化直接 JSON.parse localStorage 无异常校验 | `frontend/src/store/user.js#state` |
| 401 处理只清 localStorage 未同步 Pinia 内存态 | `frontend/src/api/request.js#responseInterceptor` |
| 前后端容器均以 root 运行 | `backend/Dockerfile`、`frontend/Dockerfile` |
| .env 以默认 umask 生成，密钥文件可被本地用户读取 | `deploy/deploy.sh#生成配置` |
| 前端依赖无 lockfile 且关闭安全审计 | `frontend/Dockerfile#npm install` |

---

## Info（6，精简表）

| 问题 | 位置 |
| --- | --- |
| JwtAuthFilter 对 subject 转 Long 未捕获异常 | `security/JwtAuthFilter.java#doFilterInternal` |
| ownerId 未校验存在性，create/update 响应 ownerName 缺失 | `client/ClientService.java#apply` |
| TimeStatusRequest DTO 未使用（死代码） | `billing/dto/TimeStatusRequest.java` |
| 日程缺少时间范围与起止时间校验 | `calendar/CalendarEvent.java` |
| 生产镜像打包 H2 运行时依赖 | `backend/pom.xml#h2` |
| 构建阶段吞错并跳过测试 | `backend/Dockerfile#构建` |

---

## 分模块小结

| 模块 | 主要风险 |
| --- | --- |
| 认证/授权/用户（auth+security+user） | 硬编码默认 JWT 密钥、H2 控制台空密码放行、异常信息外泄、token 走 URL、全员 PII 枚举、改密后旧 token 不失效 |
| 客户（client） | 完全无归属/角色校验，越权读写删全量客户 PII；删除客户遗留孤儿案件 |
| 案件（cases） | 全端点 IDOR 越权；案号并发重号、N+1、孤儿进程、状态流转无校验 |
| 计费（billing） | 账单状态变更与列表/详情缺权限；编号重号、N+1、rate 可空少计金额 |
| 文档（document） | 上传路径穿越任意写文件；详情/版本/下载越权读；版本竞态、N+1 |
| 日程/审批/知识库 | 鉴权整体健全；N+1、审批/浏览量并发竞态、停用模板仍可发起、日程全员可见 |
| 通用/配置/仪表盘 | 仪表盘无角色鉴权、DataSeeder 公开默认口令、全局异常回显内部信息 |
| 前端 | JWT 拼 URL、明文存 localStorage、adminOnly 无守卫 |
| 部署与构建 | JWT 默认密钥回退、生产默认口令、开放 Swagger、ddl-auto:update、仅 HTTP、容器 root、.env 权限过宽 |

## 修复优先级建议

1. **立即**：移除默认 JWT 密钥与默认口令（启动 fail-fast）；删除 H2 console 的 permitAll；修复文件上传路径穿越。
2. **高**：为各模块补齐方法级角色 + 归属（数据范围）鉴权（客户/案件/文档/账单/仪表盘）；修复全局异常信息回显。
3. **中**：分页参数校验、批量预取消除 N+1、编号改用序列/序列表、外键与孤儿数据治理、状态机校验。
4. **低**：登录限流、CORS 白名单、生产 ddl-auto:validate + Flyway、容器非 root、.env 权限、TLS、依赖锁定。
