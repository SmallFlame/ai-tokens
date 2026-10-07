import request from './request'

export const getLogList   = params => request.get('/logs', { params })
export const getLogDetail = id     => request.get(`/logs/${id}`)
