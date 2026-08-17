import request from './request'

export const pageArticles = (params) => request.get('/knowledge', { params })
export const getArticle = (id) => request.get(`/knowledge/${id}`)
export const createArticle = (data) => request.post('/knowledge', data)
export const updateArticle = (id, data) => request.put(`/knowledge/${id}`, data)
export const deleteArticle = (id) => request.delete(`/knowledge/${id}`)
