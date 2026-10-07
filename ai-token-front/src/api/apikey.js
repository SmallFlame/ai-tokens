import request from './request'

// 用户自己的 Key（用户门户使用）
export const getKeyList       = ()         => request.get('/keys')
export const createKey        = data       => request.post('/keys', data)
export const revokeKey        = id         => request.delete(`/keys/${id}`)
export const updateKeyChannel = (id, channelId) => request.put(`/keys/${id}/channel`, null, { params: { channelId } })

// 管理员 Key 管理（管理后台使用）
export const adminGetAllKeys      = (userId)         => request.get('/admin/keys', { params: { userId } })
export const adminCreateKey       = (userId, data)   => request.post('/admin/keys', data, { params: { userId } })
export const adminUpdateKey       = (id, data)       => request.put(`/admin/keys/${id}`, data)
export const adminUpdateKeyStatus = (id, status)     => request.put(`/admin/keys/${id}/status`, null, { params: { status } })
export const adminUpdateKeyChannel = (id, channelId) => request.put(`/admin/keys/${id}/channel`, null, { params: { channelId } })
export const adminRevokeKey       = id               => request.delete(`/admin/keys/${id}`)
export const adminBatchAssign     = data             => request.post('/admin/keys/batch-assign', data)
