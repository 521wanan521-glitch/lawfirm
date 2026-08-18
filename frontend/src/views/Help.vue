<template>
  <div class="help-page">
    <el-tabs v-model="activeTab" class="help-tabs">
      <el-tab-pane label="系统概览" name="overview" />
      <el-tab-pane label="功能模块" name="modules" />
      <el-tab-pane label="角色与权限" name="roles" />
      <el-tab-pane label="AI 助手" name="assistant" />
      <el-tab-pane label="常见问题" name="faq" />
    </el-tabs>

    <div class="help-body">
      <div class="markdown-body" v-html="renderMarkdown(currentContent)"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const activeTab = ref('overview')

const SECTIONS = {
  overview: `
# 律所数字化办公系统

面向律师事务所的一体化数字化办公平台，覆盖案件、客户、计费、文档、日程、审批、知识库与经营统计全流程，并内置 AI 智能助手。

## 访问方式

| 方式 | 说明 |
| --- | --- |
| 网页端 | 浏览器访问系统地址登录 |
| 桌面客户端 | Windows 安装程序，独立窗口、系统托盘、开机自启 |
| PWA | 浏览器"安装应用"（需 HTTPS） |

## 功能模块一览

案件管理 · 客户管理 CRM · 工时计费 · 文档中心 · 日程安排 · 审批流程 · 知识库 · 统计报表 · 成员管理 · AI 助手

## 使用建议

1. 首次登录后立即修改初始密码
2. 使用 AI 助手前，先在"模型设置"中配置自己的模型 Key
3. 所有写操作（AI 发起）均需人工确认后才会生效
`,

  modules: `
# 功能模块说明

| 模块 | 说明 |
| --- | --- |
| 工作台 | 登录首页，展示待办与关键指标 |
| 案件管理 | 立案、承办、进程跟踪、状态流转、自动案号 |
| 客户管理 | 客户档案、联系人、跟进记录、客户分级 |
| 工时计费 | 工时记录、审核流转、账单生成与收款 |
| 文档中心 | 多级目录、上传下载、多版本管理 |
| 日程安排 | 月/周/日视图、开庭/会议/任务/提醒 |
| 审批流程 | 用章/请假/报销/立案等模板，发起→审批→归档 |
| 知识库 | 办案经验、法规、文书模板沉淀 |
| 统计报表 | 案件分布、月度趋势、律师工时排行 |
| 成员管理 | 角色权限、账号启停、密码重置 |

## 案件状态流转

待立案 → 办理中 → 暂停 → 已结案 → 已归档

## 工时状态流转

已提交（待审核）→ 已审核 → 已开票

## 账单状态流转

草稿 → 已开票 → 已收款（可作废）

## 审批状态流转

待审批 → 已通过 / 已驳回（可撤销）
`,

  roles: `
# 角色与权限

系统内置 5 种角色：

| 角色 | 定位 |
| --- | --- |
| 系统管理员 | 系统运维、全所数据、成员管理 |
| 合伙人 | 全所数据、审批、计费与经营 |
| 执业律师 | 自己承办的案件与客户 |
| 律师助理 | 协助律师办理事务 |
| 行政人员 | 行政、文档、日程、审批等 |

## 数据范围

- 管理员 / 合伙人：可查看全所数据
- 律师 / 助理 / 行政：仅可查看自己主办或协办的案件、自己负责的客户、自己的工时与日程

## 权限速查

| 操作 | 管理员 | 合伙人 | 律师/助理/行政 |
| --- | --- | --- | --- |
| 查看全所案件/客户 | ✅ | ✅ | — |
| 变更案件状态 | ✅ | ✅ | 仅主办 |
| 审核工时 | ✅ | ✅ | — |
| 开票/收款/作废账单 | ✅ | ✅ | — |
| 处理审批 | ✅ | ✅ | — |
| 查看统计报表 | ✅ | ✅ | — |
| 成员管理 | ✅ | — | — |

> 说明：**—** 表示无此权限；律师/助理/行政在各自权限范围内正常操作。
`,

  assistant: `
# AI 助手

AI 助手直接连接所内真实数据，既能查询，也能在**你确认后**办理业务。

## 能做什么

- **查询**：案件、客户、工时、日程、审批、知识库、文档、经营概况
- **起草**：律师函、起诉状、合同等（须律师审核）
- **联网搜索**：最新法律法规、司法解释、判例、新闻（附来源链接）
- **办理（写操作）**：新建/修改案件、客户、工时、账单、日程、审批、知识库、成员等

## 重要机制

1. **人工确认**：所有写操作都会生成"待确认"卡片，点"确认执行"后才真正生效；点"取消"则不做任何事
2. **无删除**：系统刻意不向 AI 提供删除功能
3. **数据范围**：AI 与普通界面权限一致，只能操作你权限内的数据
4. **链接**：回答中的链接在新窗口打开，不覆盖当前页面；左上角有"返回"按钮

## 模型设置

- 点击右上角"模型设置"，选择厂商并填写自己的 API Key（加密存储，仅本人可见）
- 未配置时无法调用；费用由各自的 Key 账号承担
- 厂商：DeepSeek（推荐）/ 通义千问 / 智谱 GLM / Kimi / OpenAI / 自定义
`,

  faq: `
# 常见问题

## 1. 为什么 AI 回答有时很慢？
复杂问题需要多步查询与生成长文，属正常；页面会显示"思考中…"，可点"停止"。

## 2. 为什么提示"操作失败"？
可能是权限不够、数据状态不允许（如已结案案件不可修改），或模型 Key 余额不足。

## 3. 为什么不能删除数据？
出于安全考虑，系统不向 AI 提供删除功能，删除请到对应页面由有权限的人操作。

## 4. AI 说错了怎么办？
重要结论以系统内实际数据为准；法律意见以执业律师判断为准；联网结论请核对来源链接。

## 5. 我不想用 AI 助手，会影响其他功能吗？
不会，AI 助手是独立增强功能，其余模块照常使用。

## 6. 忘记密码怎么办？
请联系系统管理员重置密码。
`
}

const currentContent = computed(() => SECTIONS[activeTab.value] || '')
</script>

<style scoped>
.help-page {
  background: #fff;
  border-radius: 8px;
  padding: 8px 20px 24px;
  flex: 1 0 auto;
}

.help-tabs {
  margin-bottom: 4px;
}

.help-body {
  max-width: 960px;
  line-height: 1.8;
}

.markdown-body :deep(h1) {
  font-size: 22px;
  margin: 12px 0 10px;
  color: #303133;
}

.markdown-body :deep(h2) {
  font-size: 18px;
  margin: 20px 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
}

.markdown-body :deep(h3) {
  font-size: 16px;
  margin: 16px 0 8px;
}

.markdown-body :deep(p) {
  margin: 8px 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 22px;
  margin: 8px 0;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ebeef5;
  padding: 8px 12px;
  text-align: left;
  font-size: 14px;
}

.markdown-body :deep(th) {
  background: #f5f7fa;
}

.markdown-body :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 14px;
  border-left: 4px solid #409eff;
  background: #f5f7fa;
  color: #606266;
}
</style>
