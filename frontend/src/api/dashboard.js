import request from './request'

export const getSummary = () => request.get('/dashboard/summary')
export const getStats = () => request.get('/dashboard/stats')
