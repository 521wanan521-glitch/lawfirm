import request from './request'

export const listEvents = (params) => request.get('/calendar/events', { params })
export const listMyEvents = (params) => request.get('/calendar/events/mine', { params })
export const createEvent = (data) => request.post('/calendar/events', data)
export const updateEvent = (id, data) => request.put(`/calendar/events/${id}`, data)
export const deleteEvent = (id) => request.delete(`/calendar/events/${id}`)
