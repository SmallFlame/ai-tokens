import request from './request'

/** 个人充值日志 */
export const getUserRechargeLogs = params => request.get('/admin/recharge-logs/user', { params })

/** 小组充值日志 */
export const getGroupRechargeLogs = params => request.get('/admin/recharge-logs/group', { params })
