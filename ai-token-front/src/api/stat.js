import request from './request'

export const getDashboard   = ()    => request.get('/stat/dashboard')
export const getTrend       = days  => request.get('/stat/trend', { params: { days } })
export const getChannelStat = days  => request.get('/stat/channel', { params: { days } })
export const getUserStat    = days  => request.get('/stat/users',   { params: { days } })
