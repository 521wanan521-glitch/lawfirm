import request from './request'

export const pageDocuments = (params) => request.get('/documents', { params })
export const getDocument = (id) => request.get(`/documents/${id}`)
export const getVersions = (id) => request.get(`/documents/${id}/versions`)
export const uploadDocument = (data) => request.post('/documents/upload', data, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const uploadVersion = (id, data) => request.post(`/documents/${id}/version`, data, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const deleteDocument = (id) => request.delete(`/documents/${id}`)
export const downloadUrl = (id, version) => `/api/documents/${id}/download${version ? `?version=${version}` : ''}`

export const folderTree = () => request.get('/documents/folders')
export const createFolder = (data) => request.post('/documents/folders', data)
export const deleteFolder = (id) => request.delete(`/documents/folders/${id}`)
