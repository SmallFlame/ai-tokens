import request from './request'

export const getMyInfo     = ()      => request.get('/my/info')
export const getOverview   = ()      => request.get('/my/overview')
export const getMyLogs     = params  => request.get('/my/logs', { params })
export const changePassword = data   => request.put('/my/password', data)
export const updateNickname = nickname => request.put('/my/nickname', { nickname })
export const updateEmail    = email    => request.put('/my/email', { email })
export const getChannelCostChart = (days) => request.get('/my/chart/channel-cost', { params: { days } })
export const getTrendChart       = (days) => request.get('/my/chart/trend', { params: { days } })

// 管理员建账号
export const getUserList   = params  => request.get('/admin/users', { params })
export const createUser    = data    => request.post('/admin/users', data)
export const deleteUser    = id      => request.delete(`/admin/users/${id}`)
export const updateStatus  = (id, status) => request.put(`/admin/users/${id}/status`, null, { params: { status } })
