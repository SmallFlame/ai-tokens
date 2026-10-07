import request from './request'

export const getModelList = channelId  => request.get('/admin/models', { params: { channelId } })
export const createModel  = data       => request.post('/admin/models', data)
export const updateModel  = (id, data) => request.put(`/admin/models/${id}`, data)
export const deleteModel  = id         => request.delete(`/admin/models/${id}`)
