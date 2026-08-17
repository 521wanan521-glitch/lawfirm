import request from './request'

export const pageTimeEntries = (params) => request.get('/billing/time-entries', { params })
export const createTimeEntry = (data) => request.post('/billing/time-entries', data)
export const updateTimeEntry = (id, data) => request.put(`/billing/time-entries/${id}`, data)
export const deleteTimeEntry = (id) => request.delete(`/billing/time-entries/${id}`)
export const submitTimeEntry = (id) => request.put(`/billing/time-entries/${id}/submit`)
export const approveTimeEntry = (id) => request.put(`/billing/time-entries/${id}/approve`)
export const rejectTimeEntry = (id) => request.put(`/billing/time-entries/${id}/reject`)

export const pageInvoices = (params) => request.get('/billing/invoices', { params })
export const getInvoice = (id) => request.get(`/billing/invoices/${id}`)
export const createInvoice = (data) => request.post('/billing/invoices', data)
export const updateInvoiceStatus = (id, data) => request.put(`/billing/invoices/${id}/status`, data)
export const deleteInvoice = (id) => request.delete(`/billing/invoices/${id}`)
