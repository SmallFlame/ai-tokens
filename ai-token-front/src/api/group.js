import request from './request'

export const getGroupList       = name         => request.get('/admin/groups', { params: { name } })
export const createGroup        = data         => request.post('/admin/groups', data)
export const updateGroup        = (id, data)   => request.put(`/admin/groups/${id}`, data)
export const deleteGroup        = id           => request.delete(`/admin/groups/${id}`)
export const getGroupMembers    = id           => request.get(`/admin/groups/${id}/members`)
export const addGroupMember     = (id, userId) => request.post(`/admin/groups/${id}/members`, null, { params: { userId } })
export const removeGroupMember  = (id, userId) => request.delete(`/admin/groups/${id}/members/${userId}`)
export const rechargeGroupMoneyPool = (id, delta) => request.put(`/admin/groups/${id}/money-pool`, { delta })
