import request from './request'

export const getUserList      = params          => request.get('/admin/users', { params })
export const updateUserStatus = (id, status)   => request.put(`/admin/users/${id}/status`, null, { params: { status } })
/** 管理员充值/扣减用户余额（delta>0 充值，delta<0 扣减） */
export const rechargeUserMoney = (id, delta)   => request.put(`/admin/users/${id}/recharge-money`, { delta })
/** 管理员修改用户角色 */
export const updateUserRole    = (id, roleId)  => request.put(`/admin/users/${id}/role`, null, { params: { roleId } })
/** 重置用户密码为123456 */
export const resetUserPassword = (id)         => request.put(`/admin/users/${id}/reset-password`)
/** 批量设置用户金额额度 */
export const batchSetMoney     = (userIds, moneyQuota) => request.post('/admin/users/batch-money', { userIds, moneyQuota })
/** 管理员给组长充值/扣减可分配金额池 */
export const assignLeaderMoney = (id, delta)   => request.put(`/admin/users/${id}/allocatable-money`, { delta })
