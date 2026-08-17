<template>
  <div class="assistant">
    <!-- 会话列表 -->
    <aside v-if="showSidebar" class="session-panel">
      <div class="panel-header">
        <span class="panel-title">对话</span>
        <el-button type="primary" size="small" :icon="Plus" @click="newChat">新建</el-button>
      </div>
      <div class="session-list">
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session-item"
          :class="{ active: s.id === currentSessionId }"
          @click="selectSession(s)"
        >
          <template v-if="editingId === s.id">
            <el-input
              v-model="editTitle"
              size="small"
              @keyup.enter="confirmRename(s)"
              @blur="confirmRename(s)"
            />
          </template>
          <template v-else>
            <span class="session-title" :title="s.title">{{ s.title }}</span>
            <span class="session-actions">
              <el-icon @click.stop="startRename(s)"><EditPen /></el-icon>
              <el-icon @click.stop="removeSession(s)"><Delete /></el-icon>
            </span>
          </template>
        </div>
        <el-empty v-if="!sessions.length" description="暂无对话" :image-size="60" />
      </div>
    </aside>

    <!-- 聊天区 -->
    <section class="chat-panel">
      <header class="chat-header">
        <el-icon class="toggle" @click="showSidebar = !showSidebar">
          <Fold v-if="showSidebar" />
          <Expand v-else />
        </el-icon>
        <span class="chat-title">{{ currentTitle }}</span>
        <el-tag v-if="streaming" size="small" type="warning">思考中…</el-tag>
      </header>

      <div ref="scrollRef" class="messages">
        <div v-if="!items.length" class="welcome">
          <div class="welcome-icon">⚖️</div>
          <h3>律所智能助手</h3>
          <p>可以问我：案件进展、客户信息、工时统计、日程安排、知识库检索、起草文书……</p>
          <div class="suggestions">
            <el-button v-for="q in suggestions" :key="q" size="small" plain @click="ask(q)">
              {{ q }}
            </el-button>
          </div>
        </div>

        <div v-for="(item, idx) in items" :key="idx" class="msg" :class="item.kind">
          <div v-if="item.kind === 'assistant'" class="avatar">AI</div>
          <div class="bubble-wrap">
            <!-- 工具调用步骤 -->
            <div v-if="item.kind === 'assistant' && item.tools?.length" class="tools">
              <div
                v-for="t in item.tools"
                :key="t.id || t.name"
                class="tool-card"
                :class="{ fail: t.ok === false, pending: t.status === 'pending' }"
                @click="t.expanded = !t.expanded"
              >
                <div class="tool-head">
                  <el-icon class="tool-icon" :class="{ spin: t.status === 'running' }">
                    <Loading v-if="t.status === 'running'" />
                    <WarningFilled v-else-if="t.status === 'pending'" />
                    <CircleCheck v-else-if="t.ok !== false && t.status !== 'cancelled'" />
                    <CircleClose v-else />
                  </el-icon>
                  <span class="tool-name">{{ toolLabel(t.name) }}</span>
                  <span class="tool-status">{{ toolStatusText(t) }}</span>
                  <el-icon class="tool-arrow"><ArrowDown v-if="!t.expanded" /><ArrowUp v-else /></el-icon>
                </div>
                <div v-if="t.summary" class="tool-summary">{{ t.summary }}</div>
                <div v-if="t.status === 'pending'" class="tool-actions">
                  <el-button size="small" type="success" :icon="CircleCheck" @click.stop="confirmTool(t)">
                    确认执行
                  </el-button>
                  <el-button size="small" :icon="CircleClose" @click.stop="cancelTool(t)">取消</el-button>
                </div>
                <div v-if="t.expanded" class="tool-body">
                  <div class="tool-sec">
                    <span class="label">参数</span>
                    <pre>{{ prettyJson(t.arguments) }}</pre>
                  </div>
                  <div v-if="t.result" class="tool-sec">
                    <span class="label">结果</span>
                    <pre>{{ prettyJson(t.result) }}</pre>
                  </div>
                </div>
              </div>
            </div>

            <div
              v-if="item.content"
              class="bubble markdown-body"
              v-html="renderMarkdown(item.content)"
            ></div>

            <div
              v-if="item.kind === 'assistant' && streaming && idx === items.length - 1 && !item.content && !(item.tools?.length)"
              class="typing"
            >
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>
        <div ref="messagesEndRef"></div>
      </div>

      <footer class="composer">
        <el-input
          v-model="input"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 6 }"
          placeholder="输入问题，Enter 发送，Shift+Enter 换行"
          @keydown.enter.exact.prevent="send"
        />
        <el-button v-if="streaming" :icon="VideoPause" @click="stopStream">停止</el-button>
        <el-button v-else type="primary" :icon="Promotion" :disabled="!input.trim()" @click="send">
          发送
        </el-button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  ArrowUp,
  CircleCheck,
  CircleClose,
  Delete,
  EditPen,
  Expand,
  Fold,
  Loading,
  Plus,
  Promotion,
  VideoPause,
  WarningFilled
} from '@element-plus/icons-vue'
import {
  cancelAction,
  chatStream,
  confirmAction,
  deleteSession,
  listMessages,
  listPendingActions,
  listSessions,
  renameSession
} from '@/api/assistant'
import { renderMarkdown } from '@/utils/markdown'

const TOOL_LABELS = {
  list_my_cases: '查询案件',
  get_case_detail: '案件详情',
  search_clients: '搜索客户',
  get_client_detail: '客户详情',
  get_my_time_entries: '工时统计',
  record_time_entry: '记录工时',
  get_my_schedule: '查询日程',
  create_calendar_event: '创建日程',
  search_knowledge: '检索知识库',
  search_documents: '搜索文档',
  get_todo_approvals: '待办审批',
  list_approval_templates: '审批模板',
  list_approvers: '审批人',
  create_approval: '发起审批',
  add_case_progress: '记录进展',
  get_dashboard_summary: '经营概况',
  create_case: '新建案件',
  update_case: '修改案件',
  update_case_status: '变更案件状态',
  create_client: '新建客户',
  update_client: '修改客户',
  add_client_interaction: '添加跟进',
  add_client_contact: '添加联系人',
  update_client_contact: '修改联系人',
  update_time_entry: '修改工时',
  create_invoice: '创建账单',
  update_invoice_status: '变更账单状态',
  update_calendar_event: '修改日程',
  decide_approval: '审批决定',
  cancel_approval: '撤销审批',
  create_folder: '新建目录',
  create_knowledge_article: '发布知识文章',
  update_knowledge_article: '修改知识文章',
  create_user: '新建成员',
  update_user: '修改成员',
  reset_user_password: '重置密码'
}

const suggestions = [
  '我的案件有哪些？',
  '我这周记了多少工时？',
  '最近 7 天有什么日程？',
  '待我审批的审批单有哪些？',
  '检索一下合同审查要点',
  '查一下经营概况'
]

const sessions = ref([])
const currentSessionId = ref(null)
const items = ref([])
const input = ref('')
const streaming = ref(false)
const showSidebar = ref(true)
const editingId = ref(null)
const editTitle = ref('')

const scrollRef = ref()
const messagesEndRef = ref()
let controller = null

const currentTitle = computed(() => {
  if (!currentSessionId.value) return '新对话'
  const s = sessions.value.find((x) => x.id === currentSessionId.value)
  return s?.title || '新对话'
})

onMounted(loadSessions)

async function loadSessions() {
  try {
    sessions.value = await listSessions()
  } catch (e) {
    /* 未登录等情况由拦截器处理 */
  }
}

function newChat() {
  stopStream()
  currentSessionId.value = null
  items.value = []
  editingId.value = null
}

async function selectSession(s) {
  if (streaming.value || editingId.value) return
  currentSessionId.value = s.id
  try {
    const msgs = await listMessages(s.id)
    items.value = (msgs || []).map((m) => ({
      kind: m.role === 'user' ? 'user' : 'assistant',
      content: m.content,
      tools: []
    }))
    // 恢复本会话内尚未处理的待确认操作
    const actions = await listPendingActions(s.id)
    if (actions?.length) {
      items.value.push({
        kind: 'assistant',
        content: '',
        tools: actions.map((x) => ({
          id: 'action-' + x.id,
          name: x.toolName,
          arguments: x.arguments,
          status: 'pending',
          ok: null,
          result: null,
          expanded: false,
          actionId: x.id,
          summary: x.summary
        }))
      })
    }
    scrollToBottom()
  } catch (e) {
    /* ignore */
  }
}

function ask(q) {
  input.value = q
  send()
}

async function send() {
  const text = input.value.trim()
  if (!text || streaming.value) return
  input.value = ''

  items.value.push({ kind: 'user', content: text })
  items.value.push({ kind: 'assistant', content: '', tools: [] })
  // 从响应式数组取回代理对象，保证后续 content/tools 的修改能触发视图更新
  const assistantItem = items.value[items.value.length - 1]

  streaming.value = true
  controller = new AbortController()
  scrollToBottom()

  try {
    await chatStream({
      sessionId: currentSessionId.value,
      message: text,
      onEvent: (event, data) => handleEvent(event, data, assistantItem),
      signal: controller.signal
    })
  } catch (e) {
    if (e.name !== 'AbortError') {
      const msg = e.message || '对话失败'
      ElMessage.error(msg)
      assistantItem.content += `\n\n> ⚠️ ${msg}`
    }
  } finally {
    streaming.value = false
    controller = null
    loadSessions()
    scrollToBottom()
  }
}

function handleEvent(event, data, assistantItem) {
  switch (event) {
    case 'meta':
      currentSessionId.value = data.sessionId
      loadSessions()
      break
    case 'delta':
      assistantItem.content += data.content || ''
      scrollToBottom()
      break
    case 'tool':
      assistantItem.tools.push({
        id: data.id,
        name: data.name,
        arguments: data.arguments,
        status: data.pending ? 'pending' : 'running',
        ok: null,
        result: null,
        expanded: false,
        actionId: data.actionId || null,
        summary: data.summary || ''
      })
      scrollToBottom()
      break
    case 'tool_result': {
      const t = assistantItem.tools.find((x) => x.id === data.id)
      if (t) {
        // 待确认类写操作：保持「待确认」状态等待用户点击确认/取消，不标记为完成
        if (t.status === 'pending') break
        t.status = 'done'
        t.ok = data.ok
        t.result = data.result
      }
      break
    }
    case 'error':
      assistantItem.content += `\n\n> ⚠️ ${data.message || '发生错误'}`
      break
    case 'done':
    default:
      break
  }
}

function stopStream() {
  if (controller) {
    controller.abort()
    controller = null
  }
  streaming.value = false
}

async function confirmTool(t) {
  if (!t.actionId) return
  try {
    const res = await confirmAction(t.actionId)
    t.status = 'done'
    t.ok = res.ok
    t.result = res.result
    if (res.ok) {
      ElMessage.success('操作已执行')
    } else {
      ElMessage.error('执行失败，详见卡片结果')
    }
  } catch (e) {
    /* ignore */
  }
}

async function cancelTool(t) {
  if (!t.actionId) return
  try {
    await cancelAction(t.actionId)
    t.status = 'cancelled'
    t.ok = false
    ElMessage.info('已取消')
  } catch (e) {
    /* ignore */
  }
}

function toolStatusText(t) {
  if (t.status === 'running') return '执行中…'
  if (t.status === 'pending') return '待确认'
  if (t.status === 'cancelled') return '已取消'
  return t.ok === false ? '失败' : '已完成'
}

function startRename(s) {
  editingId.value = s.id
  editTitle.value = s.title
}

async function confirmRename(s) {
  if (editingId.value !== s.id) return
  editingId.value = null
  const title = editTitle.value.trim()
  if (title && title !== s.title) {
    try {
      const updated = await renameSession(s.id, title)
      s.title = updated.title
    } catch (e) {
      /* ignore */
    }
  }
}

async function removeSession(s) {
  try {
    await ElMessageBox.confirm('删除该对话？', '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch (e) {
    return
  }
  await deleteSession(s.id)
  sessions.value = sessions.value.filter((x) => x.id !== s.id)
  if (currentSessionId.value === s.id) {
    newChat()
  }
}

function toolLabel(name) {
  return TOOL_LABELS[name] || name
}

function prettyJson(str) {
  if (!str) return ''
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = scrollRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>

<style scoped>
.assistant {
  display: flex;
  height: 100%;
  min-height: 0;
  background: #f7f8fa;
  border-radius: 8px;
  overflow: hidden;
}

/* 会话列表 */
.session-panel {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}

.panel-title {
  font-weight: 600;
  font-size: 14px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  min-height: 0;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  color: #333;
  font-size: 13px;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-actions {
  display: none;
  gap: 8px;
  color: #909399;
  margin-left: 8px;
}

.session-item:hover .session-actions {
  display: inline-flex;
}

.session-actions .el-icon:hover {
  color: #409eff;
}

/* 聊天区 */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.toggle {
  cursor: pointer;
  font-size: 18px;
  color: #606266;
}

.chat-title {
  flex: 1;
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  min-height: 0;
}

.welcome {
  text-align: center;
  margin-top: 8vh;
  color: #909399;
}

.welcome-icon {
  font-size: 48px;
}

.welcome h3 {
  margin: 12px 0 6px;
  color: #303133;
}

.welcome p {
  margin: 0 0 20px;
  font-size: 13px;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.msg {
  display: flex;
  margin-bottom: 16px;
}

.msg.user {
  justify-content: flex-end;
}

.avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  margin-right: 10px;
}

.bubble-wrap {
  max-width: 82%;
  min-width: 0;
}

.msg.user .bubble {
  background: #409eff;
  color: #fff;
}

.msg.assistant .bubble {
  background: #fff;
  border: 1px solid #ebeef5;
}

.bubble {
  padding: 10px 14px;
  border-radius: 10px;
  line-height: 1.7;
  font-size: 14px;
  word-break: break-word;
}

.msg.user .bubble {
  border-top-right-radius: 2px;
}

.msg.assistant .bubble {
  border-top-left-radius: 2px;
}

/* 工具卡片 */
.tools {
  margin-bottom: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tool-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px 10px;
  cursor: pointer;
  font-size: 13px;
}

.tool-card.fail {
  border-color: #fde2e2;
  background: #fef0f0;
}

.tool-card.pending {
  border-color: #faecd8;
  background: #fdf6ec;
}

.tool-card.pending .tool-icon {
  color: #e6a23c;
}

.tool-summary {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.tool-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.tool-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-icon {
  color: #67c23a;
}

.tool-card.fail .tool-icon {
  color: #f56c6c;
}

.tool-icon.spin {
  color: #409eff;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}

.tool-name {
  font-weight: 600;
  color: #303133;
}

.tool-status {
  flex: 1;
  color: #909399;
  font-size: 12px;
}

.tool-arrow {
  color: #c0c4cc;
}

.tool-body {
  margin-top: 8px;
  border-top: 1px dashed #ebeef5;
  padding-top: 8px;
}

.tool-sec {
  margin-bottom: 8px;
}

.tool-sec .label {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.tool-sec pre {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 8px;
  margin: 0;
  max-height: 220px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

/* Markdown 内容 */
.markdown-body :deep(p) {
  margin: 0 0 8px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(pre) {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 12px;
  overflow: auto;
  margin: 8px 0;
}

.markdown-body :deep(code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
}

.markdown-body :deep(p code),
.markdown-body :deep(li code) {
  background: #f0f2f5;
  padding: 1px 5px;
  border-radius: 4px;
  color: #c7254e;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ebeef5;
  padding: 6px 10px;
  font-size: 13px;
}

.markdown-body :deep(th) {
  background: #f5f7fa;
}

.markdown-body :deep(blockquote) {
  margin: 8px 0;
  padding: 4px 12px;
  border-left: 3px solid #dcdfe6;
  color: #909399;
}

.msg.user .markdown-body :deep(p) {
  margin: 0;
}

.typing {
  display: inline-flex;
  gap: 4px;
  padding: 12px 14px;
}

.typing .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: blink 1.2s infinite;
}

.typing .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.3;
  }
  40% {
    opacity: 1;
  }
}

/* 输入区 */
.composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #ebeef5;
}

.composer .el-textarea {
  flex: 1;
}
</style>
