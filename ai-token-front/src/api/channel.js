import request from './request'

export const getChannelList        = ()            => request.get('/admin/channels')
export const createChannel         = data          => request.post('/admin/channels', data)
export const updateChannel         = (id, data)    => request.put(`/admin/channels/${id}`, data)
export const updateChannelStatus   = (id, status)  => request.put(`/admin/channels/${id}/status`, null, { params: { status } })
export const deleteChannel         = id            => request.delete(`/admin/channels/${id}`)
export const healthCheck           = id            => request.post(`/admin/channels/${id}/health-check`)

/** 管理员：获取/设置默认渠道 */
export const getAdminDefaultChannel = ()           => request.get('/admin/channels/default')
export const setAdminDefaultChannel = (channelId)  => request.put('/admin/channels/default', null, { params: { channelId } })

/** 渠道充值/扣减预算 */
export const rechargeChannel        = (id, delta, remark) => request.post(`/admin/channels/${id}/recharge`, { delta, remark })
/** 查询渠道充值日志 */
export const getChannelRechargeLogs = (id)         => request.get(`/admin/channels/${id}/recharge-logs`)

/** 用户端：获取可用渠道及模型价格列表 */
export const getUserChannels = () => request.get('/user/channels')

/** 用户端：获取默认渠道ID（创建Key时预填） */
export const getUserDefaultChannel = () => request.get('/user/channels/default')
