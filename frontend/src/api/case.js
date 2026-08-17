import request from './request'

export const pageCases = (params) => request.get('/cases', { params })
export const myCases = (params) => request.get('/cases/my', { params })
export const getCase = (id) => request.get(`/cases/${id}`)
export const createCase = (data) => request.post('/cases', data)
export const updateCase = (id, data) => request.put(`/cases/${id}`, data)
export const updateCaseStatus = (id, data) => request.put(`/cases/${id}/status`, data)
export const deleteCase = (id) => request.delete(`/cases/${id}`)

export const pageProgress = (id, params) => request.get(`/cases/${id}/progress`, { params })
export const addProgress = (id, data) => request.post(`/cases/${id}/progress`, data)
