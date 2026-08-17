import request from './request'

export const pageClients = (params) => request.get('/clients', { params })
export const getClient = (id) => request.get(`/clients/${id}`)
export const createClient = (data) => request.post('/clients', data)
export const updateClient = (id, data) => request.put(`/clients/${id}`, data)
export const deleteClient = (id) => request.delete(`/clients/${id}`)

export const getContacts = (id) => request.get(`/clients/${id}/contacts`)
export const addContact = (id, data) => request.post(`/clients/${id}/contacts`, data)
export const updateContact = (id, cid, data) => request.put(`/clients/${id}/contacts/${cid}`, data)
export const deleteContact = (id, cid) => request.delete(`/clients/${id}/contacts/${cid}`)

export const pageInteractions = (id, params) => request.get(`/clients/${id}/interactions`, { params })
export const addInteraction = (id, data) => request.post(`/clients/${id}/interactions`, data)
