// API 层：与 web 端后端接口一致
import { get, post, put, del } from '@/utils/request'

// ============ 认证 ============
export const login = (data) => post('/auth/login', data)
export const getMe = () => get('/auth/me')
export const changePassword = (data) => put('/auth/password', data)

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
