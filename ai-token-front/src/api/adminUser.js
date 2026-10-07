import request from './request'

export const getOutGroupUsers    = params         => request.get('/admin/admin-users', { params })
export const createAdminUser     = data           => request.post('/admin/admin-users', data)
export const updateAdminStatus   = (id, status)   => request.put(`/admin/admin-users/${id}/status`, null, { params: { status } })
export const deleteAdminUser     = id             => request.delete(`/admin/admin-users/${id}`)
export const rechargeAdminUser   = (id, delta)    => request.put(`/admin/admin-users/${id}/recharge-money`, { delta })
export const setOutGroupUserRole = (id, roleId)   => request.put(`/admin/admin-users/${id}/role`, { roleId })
export const getDuplicateUsers   = params         => request.get('/admin/admin-users/duplicates', { params })
export const getDeletedUsers     = params         => request.get('/admin/admin-users/deleted', { params })
export const restoreUser         = id             => request.put(`/admin/admin-users/${id}/restore`)
