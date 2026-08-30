// API 层：与 web 端后端接口一致
import { get, post, put, del } from '@/utils/request'

// ============ 认证 ============
export const login = (data) => post('/auth/login', data)
export const getMe = () => get('/auth/me')
export const changePassword = (data) => put('/auth/password', data)
export const updateProfile = (data) => put('/auth/profile', data)

/** 上传头像（返回更新后的 UserInfo） */
export function uploadAvatar(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: 'http://47.107.62.86/api/auth/avatar',
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${uni.getStorageSync('token')}` },
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 0) resolve(body.data)
          else reject(new Error(body.message || '上传失败'))
        } catch (e) {
          reject(new Error('上传失败'))
        }
      },
      fail: reject
    })
  })
}

// ============ 工作台 ============
export const getSummary = () => get('/dashboard/summary')

// ============ 案件 ============
export const pageCases = (params) => get('/cases', params)
export const getCase = (id) => get(`/cases/${id}`)
export const createCase = (data) => post('/cases', data)
export const updateCase = (id, data) => put(`/cases/${id}`, data)
export const updateCaseStatus = (id, data) => put(`/cases/${id}/status`, data)
export const pageProgress = (id, params) => get(`/cases/${id}/progress`, params)

// ============ 客户 ============
export const pageClients = (params) => get('/clients', params)
export const getClient = (id) => get(`/clients/${id}`)
export const createClient = (data) => post('/clients', data)
export const getContacts = (id) => get(`/clients/${id}/contacts`)
export const pageInteractions = (id, params) => get(`/clients/${id}/interactions`, params)

// ============ 日程 ============
export const listEvents = (params) => get('/calendar/events', params)
export const createEvent = (data) => post('/calendar/events', data)
export const deleteEvent = (id) => del(`/calendar/events/${id}`)

// ============ 审批 ============
export const listTemplates = () => get('/approvals/templates')
export const listApprovers = () => get('/approvals/approvers')
export const pageInstances = (params) => get('/approvals/instances', params)
export const createInstance = (data) => post('/approvals/instances', data)
export const approveInstance = (id, data) => put(`/approvals/instances/${id}/approve`, data)
export const rejectInstance = (id, data) => put(`/approvals/instances/${id}/reject`, data)
export const cancelInstance = (id) => put(`/approvals/instances/${id}/cancel`)

// ============ 用户选项（下拉用） ============
export const userOptions = () => get('/users/options')
export const caseOptions = () => get('/cases/my', { page: 1, size: 200 })

// ============ 工时 ============
export const pageTimeEntries = (params) => get('/billing/time-entries', params)
export const createTimeEntry = (data) => post('/billing/time-entries', data)
export const submitTimeEntry = (id) => put(`/billing/time-entries/${id}/submit`)
export const approveTimeEntry = (id) => put(`/billing/time-entries/${id}/approve`)
export const rejectTimeEntry = (id) => put(`/billing/time-entries/${id}/reject`)

// ============ 账单 ============
export const pageInvoices = (params) => get('/billing/invoices', params)

// ============ 文档 ============
export const pageDocuments = (params) => get('/documents', params)
export const getVersions = (id) => get(`/documents/${id}/versions`)
export const folderTree = () => get('/documents/folders')

// ============ 知识库 ============
export const pageArticles = (params) => get('/knowledge', params)
export const getArticle = (id) => get(`/knowledge/${id}`)

// ============ 统计 ============
export const getStats = () => get('/dashboard/stats')

// ============ 成员 ============
export const pageUsers = (params) => get('/users', params)
export const resetPassword = (id, data) => put(`/users/${id}/password`, data)
export const setUserStatus = (id, enabled) => put(`/users/${id}/status`, null, { params: { enabled } })

// ============ AI 助手 ============
export const listSessions = () => get('/assistant/sessions')
export const listMessages = (id) => get(`/assistant/sessions/${id}/messages`)
export const listPendingActions = (sessionId) => get('/assistant/actions', { sessionId })
export const confirmAction = (id) => post(`/assistant/actions/${id}/confirm`)
export const cancelAction = (id) => post(`/assistant/actions/${id}/cancel`)
export const getLlmConfig = () => get('/assistant/llm-config')
export const saveLlmConfig = (data) => put('/assistant/llm-config', data)
