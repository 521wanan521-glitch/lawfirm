import request from './request'

export const listTemplates = (all = false) => request.get('/approvals/templates', { params: { all } })
export const createTemplate = (data) => request.post('/approvals/templates', data)
export const updateTemplate = (id, data) => request.put(`/approvals/templates/${id}`, data)
export const setTemplateEnabled = (id, enabled) => request.put(`/approvals/templates/${id}/status`, null, { params: { enabled } })
export const deleteTemplate = (id) => request.delete(`/approvals/templates/${id}`)
export const listApprovers = () => request.get('/approvals/approvers')

export const pageInstances = (params) => request.get('/approvals/instances', { params })
export const createInstance = (data) => request.post('/approvals/instances', data)
export const approveInstance = (id, data) => request.put(`/approvals/instances/${id}/approve`, data)
export const rejectInstance = (id, data) => request.put(`/approvals/instances/${id}/reject`, data)
export const cancelInstance = (id) => request.put(`/approvals/instances/${id}/cancel`)
