import request from './request'

export const pageUsers = (params) => request.get('/users', { params })
export const userOptions = () => request.get('/users/options')
export const createUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const setUserStatus = (id, enabled) => request.put(`/users/${id}/status`, null, { params: { enabled } })
export const resetPassword = (id, data) => request.put(`/users/${id}/password`, data)
export const deleteUser = (id) => request.delete(`/users/${id}`)
