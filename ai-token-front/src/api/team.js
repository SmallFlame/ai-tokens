import request from './request'

// ── 成员管理 ──
export const getTeamMembers      = ()              => request.get('/team/members')
export const createTeamMember    = data            => request.post('/team/members', data)
export const updateMemberStatus  = (id, status)    => request.put(`/team/members/${id}/status`, null, { params: { status } })
export const deleteTeamMember    = id              => request.delete(`/team/members/${id}`)

// ── Key 管理 ──
export const getTeamKeys         = ()              => request.get('/team/keys')
export const createTeamKey       = (targetUserId, data) => request.post('/team/keys', data, { params: { targetUserId } })
export const revokeTeamKey       = id              => request.delete(`/team/keys/${id}`)

// ── 额度查询 ──
export const getMyQuota          = ()              => request.get('/team/quota')

// ── 拉人 / 释放 / 金额分配 ──
export const getAvailableUsers   = ()              => request.get('/team/available-members')
export const pullMember          = userId          => request.post(`/team/pull/${userId}`)
export const releaseMember       = userId          => request.delete(`/team/release/${userId}`)
export const adjustMemberMoney   = (id, delta)      => request.put(`/team/members/${id}/adjust-money`, { delta })
